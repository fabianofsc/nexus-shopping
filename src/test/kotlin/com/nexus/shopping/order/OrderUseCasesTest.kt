package com.nexus.shopping.order

import com.nexus.shopping.cart.application.port.inbound.CartCheckoutInputPort
import com.nexus.shopping.cart.application.port.inbound.CartCheckoutReservation
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartItem
import com.nexus.shopping.cart.domain.CartStatus
import com.nexus.shopping.cart.domain.ProductSummary
import com.nexus.shopping.order.application.command.CheckoutOrderCommand
import com.nexus.shopping.order.application.exception.OrderNotFoundException
import com.nexus.shopping.order.application.exception.OrderValidationException
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.port.outbound.TransactionPort
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

    override fun findById(id: Long): Order? = ordersById[id]

    override fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): Order? = ordersByIdempotencyKey[customerId to idempotencyKey]

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Order> {
        val all = ordersById.values.filter { it.customerId == customerId }.sortedBy { it.id }
        val content = all.drop(page * size).take(size)
        return PageResult(content, page, size, content.size, (page + 1) * size < all.size)
    }

    override fun create(order: Order): Order {
        val key = order.customerId to order.idempotencyKey
        return ordersByIdempotencyKey[key] ?: persist(order).also { ordersByIdempotencyKey[key] = it }
    }

    override fun update(order: Order): Order = persist(order)

    private fun persist(order: Order): Order {
        val persisted =
            Order(
                id = order.id ?: nextId++,
                customerId = order.customerId,
                cartId = order.cartId,
                customerSnapshot = order.customerSnapshot,
                shippingAddressSnapshot = order.shippingAddressSnapshot,
                items = order.items,
                status = order.status,
                idempotencyKey = order.idempotencyKey,
                requestFingerprint = order.requestFingerprint,
                createdAt = order.createdAt ?: Instant.parse("2026-07-24T12:00:00Z"),
                cancelledAt = order.cancelledAt,
            )
        ordersById[requireNotNull(persisted.id)] = persisted
        return persisted
    }
}

private class FakeCartCheckout(
    private val items: List<OrderItemSnapshot>,
) : CartCheckoutInputPort {
    override fun reserveActiveCart(customerId: Long): CartCheckoutReservation =
        CartCheckoutReservation(
            Cart(
                id = 100L,
                customerId = customerId,
                status = CartStatus.ACTIVE,
                items =
                    items.map { item ->
                        CartItem(
                            ProductSummary(
                                item.productId,
                                item.productName,
                                item.unitPriceAmount,
                                com.nexus.shopping.cart.domain.Currency
                                    .valueOf(item.currency.name),
                            ),
                            item.quantity,
                        )
                    },
                createdAt = null,
                updatedAt = null,
            ),
        )

    override fun confirmCheckout(reservationId: Long) = Unit
}

private object FakeTransaction : TransactionPort {
    override fun <T> inTransaction(block: () -> T): T = block()
}

class OrderUseCasesTest {
    private fun checkoutCommand() =
        CheckoutOrderCommand(
            customerId = 10L,
            customerSnapshot = CustomerSnapshot(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null),
            shippingAddressSnapshot =
                ShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
            idempotencyKey = "checkout-1",
        )

    private fun item() = OrderItemSnapshot(1L, "Produto A", BigDecimal("19.90"), Currency.BRL, 2)

    private fun checkoutUseCase(
        repository: OrderRepositoryPort,
        items: List<OrderItemSnapshot> = listOf(item()),
    ) = CheckoutOrderUseCase(repository, FakeCartCheckout(items), FakeTransaction)

    @Test
    fun `checkout creates an order waiting for payment from snapshots`() {
        val repository = FakeOrderRepository()

        val order = checkoutUseCase(repository).execute(checkoutCommand())

        assertEquals(OrderStatus.WAITING_PAYMENT, order.status)
        assertEquals(BigDecimal("39.80"), order.totalAmount)
        assertEquals("Ana Silva", order.customerSnapshot.name)
    }

    @Test
    fun `checkout isolates the order from later mutations to the locked item list`() {
        val items = mutableListOf(item())
        val order = checkoutUseCase(FakeOrderRepository(), items).execute(checkoutCommand())
        items.clear()

        assertEquals(1, order.items.size)
        assertEquals(BigDecimal("39.80"), order.totalAmount)
    }

    @Test
    fun `checkout rejects an empty cart snapshot before creating an order`() {
        assertFailsWith<OrderValidationException> {
            checkoutUseCase(FakeOrderRepository(), emptyList()).execute(checkoutCommand())
        }
    }

    @Test
    fun `checkout rejects a cart snapshot with multiple currencies`() {
        val items =
            listOf(
                item(),
                OrderItemSnapshot(2L, "Produto B", BigDecimal("10.00"), Currency.USD, 1),
            )

        assertFailsWith<OrderValidationException> {
            checkoutUseCase(FakeOrderRepository(), items).execute(checkoutCommand())
        }
    }

    @Test
    fun `checkout rejects a snapshot owned by another customer`() {
        assertFailsWith<OrderValidationException> {
            checkoutUseCase(FakeOrderRepository()).execute(
                checkoutCommand().copy(customerSnapshot = checkoutCommand().customerSnapshot.copy(customerId = 20L)),
            )
        }
    }

    @Test
    fun `checkout requires a nonblank idempotency key`() {
        assertFailsWith<OrderValidationException> {
            checkoutUseCase(FakeOrderRepository()).execute(checkoutCommand().copy(idempotencyKey = " "))
        }
    }

    @Test
    fun `gets an order by id and lists orders for its customer`() {
        val repository = FakeOrderRepository()
        val created = checkoutUseCase(repository).execute(checkoutCommand())

        val found = GetOrderByIdUseCase(repository).execute(requireNotNull(created.id))
        val page = ListOrdersByCustomerUseCase(repository).list(customerId = 10L, page = 0, size = 10)

        assertEquals(created, found)
        assertEquals(listOf(created), page.content)
    }

    @Test
    fun `rejects invalid order page requests before accessing the repository`() {
        val useCase = ListOrdersByCustomerUseCase(FakeOrderRepository())

        assertFailsWith<OrderValidationException> { useCase.list(customerId = 10L, page = -1, size = 50) }
        assertFailsWith<OrderValidationException> { useCase.list(customerId = 10L, page = 0, size = 0) }
        assertFailsWith<OrderValidationException> { useCase.list(customerId = 10L, page = 0, size = 501) }
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
        val created = checkoutUseCase(repository).execute(checkoutCommand())

        val cancelled = CancelOrderUseCase(repository).execute(requireNotNull(created.id))

        assertEquals(OrderStatus.CANCELLED, cancelled.status)
        assertEquals(created.cartId, cancelled.cartId)
    }
}
