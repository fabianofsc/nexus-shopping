package com.nexus.shopping.order

import com.nexus.shopping.order.application.command.CheckoutOrderCommand
import com.nexus.shopping.order.application.exception.OrderIdempotencyConflictException
import com.nexus.shopping.order.application.exception.OrderNotFoundException
import com.nexus.shopping.order.application.exception.OrderValidationException
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.usecase.CancelOrderUseCase
import com.nexus.shopping.order.application.usecase.CheckoutOrderUseCase
import com.nexus.shopping.order.application.usecase.GetOrderByIdUseCase
import com.nexus.shopping.order.application.usecase.ListOrdersByCustomerUseCase
import com.nexus.shopping.order.domain.Currency
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.OrderStatus
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
import com.nexus.shopping.platform.domain.PageResult
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeOrderRepository : OrderRepositoryPort {
    private val ordersById = mutableMapOf<Long, Order>()
    private val ordersByIdempotencyKey = mutableMapOf<Pair<Long, String>, Order>()
    private var nextId = 1L
    var createdOrders = 0
        private set

    override fun findById(id: Long): Order? = ordersById[id]

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Order> {
        val all = ordersById.values.filter { it.customerId == customerId }.sortedBy { it.id }
        val content = all.drop(page * size).take(size)
        return PageResult(content, page, size, content.size, (page + 1) * size < all.size)
    }

    override fun createIfAbsentByCustomerIdAndIdempotencyKey(order: Order): Order {
        val key = order.customerId to order.idempotencyKey
        return ordersByIdempotencyKey[key] ?: persist(order).also {
            ordersByIdempotencyKey[key] = it
            createdOrders++
        }
    }

    override fun update(order: Order): Order = persist(order)

    private fun persist(order: Order): Order {
        val persisted =
            order.copy(
                id = order.id ?: nextId++,
                createdAt = order.createdAt ?: Instant.parse("2026-07-24T12:00:00Z"),
            )
        ordersById[requireNotNull(persisted.id)] = persisted
        return persisted
    }
}

class OrderUseCasesTest {
    private fun checkoutCommand(fingerprint: String = "fingerprint-1") =
        CheckoutOrderCommand(
            cartId = 100L,
            customerId = 10L,
            customerSnapshot = CustomerSnapshot(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null),
            shippingAddressSnapshot =
                ShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
            items = listOf(OrderItemSnapshot(1L, "Produto A", BigDecimal("19.90"), Currency.BRL, 2)),
            idempotencyKey = "checkout-1",
            requestFingerprint = fingerprint,
        )

    @Test
    fun `checkout creates an order waiting for payment from snapshots`() {
        val repository = FakeOrderRepository()

        val order = CheckoutOrderUseCase(repository).execute(checkoutCommand())

        assertEquals(OrderStatus.WAITING_PAYMENT, order.status)
        assertEquals(BigDecimal("39.80"), order.totalAmount)
        assertEquals("Ana Silva", order.customerSnapshot.name)
        assertEquals(1, repository.createdOrders)
    }

    @Test
    fun `checkout replays the original order for the same customer key and fingerprint`() {
        val repository = FakeOrderRepository()
        val useCase = CheckoutOrderUseCase(repository)
        val original = useCase.execute(checkoutCommand())

        val replay = useCase.execute(checkoutCommand())

        assertEquals(original, replay)
        assertEquals(1, repository.createdOrders)
    }

    @Test
    fun `checkout rejects reuse of a customer idempotency key with a different fingerprint`() {
        val repository = FakeOrderRepository()
        val useCase = CheckoutOrderUseCase(repository)
        useCase.execute(checkoutCommand())

        assertFailsWith<OrderIdempotencyConflictException> {
            useCase.execute(checkoutCommand(fingerprint = "different-fingerprint"))
        }
    }

    @Test
    fun `checkout rejects an empty cart snapshot before creating an order`() {
        val repository = FakeOrderRepository()

        assertFailsWith<OrderValidationException> {
            CheckoutOrderUseCase(repository).execute(checkoutCommand().copy(items = emptyList()))
        }

        assertEquals(0, repository.createdOrders)
    }

    @Test
    fun `checkout rejects a snapshot owned by another customer`() {
        val repository = FakeOrderRepository()

        assertFailsWith<OrderValidationException> {
            CheckoutOrderUseCase(repository).execute(
                checkoutCommand().copy(customerSnapshot = checkoutCommand().customerSnapshot.copy(customerId = 20L)),
            )
        }

        assertEquals(0, repository.createdOrders)
    }

    @Test
    fun `checkout requires a nonblank idempotency key`() {
        val repository = FakeOrderRepository()

        assertFailsWith<OrderValidationException> {
            CheckoutOrderUseCase(repository).execute(checkoutCommand().copy(idempotencyKey = " "))
        }

        assertEquals(0, repository.createdOrders)
    }

    @Test
    fun `gets an order by id and lists orders for its customer`() {
        val repository = FakeOrderRepository()
        val created = CheckoutOrderUseCase(repository).execute(checkoutCommand())

        val found = GetOrderByIdUseCase(repository).execute(requireNotNull(created.id))
        val page = ListOrdersByCustomerUseCase(repository).list(customerId = 10L, page = 0, size = 10)

        assertEquals(created, found)
        assertEquals(listOf(created), page.content)
    }

    @Test
    fun `throws not found when the requested order does not exist`() {
        assertFailsWith<OrderNotFoundException> {
            GetOrderByIdUseCase(FakeOrderRepository()).execute(999L)
        }
    }

    @Test
    fun `cancels an order waiting for payment without changing its cart`() {
        val repository = FakeOrderRepository()
        val created = CheckoutOrderUseCase(repository).execute(checkoutCommand())

        val cancelled = CancelOrderUseCase(repository).execute(requireNotNull(created.id))

        assertEquals(OrderStatus.CANCELLED, cancelled.status)
        assertEquals(created.cartId, cancelled.cartId)
    }
}
