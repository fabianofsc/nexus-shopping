package com.nexus.shopping.order

import com.nexus.shopping.order.application.command.CreateOrderCommand
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.usecase.CreateOrderUseCase
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

private class CreateOrderRepositoryFake : OrderRepositoryPort {
    private val ordersByKey = mutableMapOf<Pair<Long, String>, Order>()
    private var nextId = 1L

    override fun findById(id: Long): Order? = ordersByKey.values.firstOrNull { it.id == id }

    override fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): Order? = ordersByKey[customerId to idempotencyKey]

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Order> = PageResult(emptyList(), page, size, 0, false)

    override fun create(order: Order): Order {
        val key = order.customerId to order.idempotencyKey
        return ordersByKey[key] ?: persist(order).also { ordersByKey[key] = it }
    }

    override fun update(order: Order): Order = order

    private fun persist(order: Order) =
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
            createdAt = Instant.parse("2026-07-26T12:00:00Z"),
            cancelledAt = order.cancelledAt,
        )
}

class CreateOrderUseCaseTest {
    private fun command(items: List<OrderItemSnapshot> = listOf(item())) =
        CreateOrderCommand(
            customerId = 10L,
            cartId = 100L,
            customerSnapshot = CustomerSnapshot(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null),
            shippingAddressSnapshot =
                ShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
            items = items,
            idempotencyKey = "checkout-1",
        )

    private fun item() = OrderItemSnapshot(1L, "Produto A", BigDecimal("19.90"), Currency.BRL, 2)

    @Test
    fun `creates an order from the supplied snapshots and item snapshots`() {
        val created = CreateOrderUseCase(CreateOrderRepositoryFake()).create(command())

        assertEquals(false, created.replayed)
        assertEquals(100L, created.order.cartId)
        assertEquals("Ana Silva", created.order.customerSnapshot.name)
        assertEquals(listOf(item()), created.order.items)
        assertEquals(OrderStatus.WAITING_PAYMENT, created.order.status)
    }

    @Test
    fun `replays the order for the same idempotency key and payload`() {
        val useCase = CreateOrderUseCase(CreateOrderRepositoryFake())

        val original = useCase.create(command())
        val replay = useCase.create(command())

        assertEquals(false, original.replayed)
        assertEquals(true, replay.replayed)
        assertEquals(original.order, replay.order)
    }
}
