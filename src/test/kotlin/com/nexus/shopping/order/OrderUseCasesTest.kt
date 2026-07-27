package com.nexus.shopping.order

import com.nexus.shopping.order.application.exception.OrderNotFoundException
import com.nexus.shopping.order.application.exception.OrderValidationException
import com.nexus.shopping.order.application.port.outbound.OrderPersistenceResult
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.usecase.CancelOrderUseCase
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
    private val ordersByKey = mutableMapOf<Pair<Long, String>, Order>()
    private var nextId = 1L

    override fun findById(id: Long): Order? = ordersById[id]

    override fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): Order? = ordersByKey[customerId to idempotencyKey]

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Order> {
        val all = ordersById.values.filter { it.customerId == customerId }.sortedBy { it.id }
        val content = all.drop(page * size).take(size)
        return PageResult(content, page, size, content.size, (page + 1) * size < all.size)
    }

    override fun create(order: Order): OrderPersistenceResult {
        val key = order.customerId to order.idempotencyKey
        val existing = ordersByKey[key]
        if (existing != null) return OrderPersistenceResult(existing, created = false)
        val persisted =
            Order(
                id = nextId++,
                customerId = order.customerId,
                cartId = order.cartId,
                customerSnapshot = order.customerSnapshot,
                shippingAddressSnapshot = order.shippingAddressSnapshot,
                items = order.items,
                status = order.status,
                idempotencyKey = order.idempotencyKey,
                requestFingerprint = order.requestFingerprint,
                createdAt = Instant.parse("2026-07-24T12:00:00Z"),
                cancelledAt = order.cancelledAt,
            )
        ordersById[requireNotNull(persisted.id)] = persisted
        ordersByKey[key] = persisted
        return OrderPersistenceResult(persisted, created = true)
    }

    override fun update(order: Order): Order = order.also { ordersById[requireNotNull(it.id)] = it }
}

class OrderUseCasesTest {
    @Test
    fun `gets an order by id and lists orders for its customer`() {
        val repository = FakeOrderRepository()
        val created = persistWaitingOrder(repository)

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
        val created = persistWaitingOrder(repository)

        val cancelled = CancelOrderUseCase(repository).execute(requireNotNull(created.id))

        assertEquals(OrderStatus.CANCELLED, cancelled.status)
        assertEquals(created.cartId, cancelled.cartId)
    }

    private fun persistWaitingOrder(repository: FakeOrderRepository): Order =
        repository
            .create(
                Order(
                    id = null,
                    customerId = 10L,
                    cartId = 100L,
                    customerSnapshot = CustomerSnapshot(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null),
                    shippingAddressSnapshot =
                        ShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
                    items = listOf(OrderItemSnapshot(1L, "Produto A", BigDecimal("19.90"), Currency.BRL, 2)),
                    status = OrderStatus.WAITING_PAYMENT,
                    idempotencyKey = "checkout-1",
                    requestFingerprint = "fingerprint",
                    createdAt = null,
                    cancelledAt = null,
                ),
            ).order
}
