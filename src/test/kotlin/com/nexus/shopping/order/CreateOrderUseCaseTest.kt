package com.nexus.shopping.order

import com.nexus.shopping.order.application.command.CreateOrderCommand
import com.nexus.shopping.order.application.port.outbound.OrderPersistenceResult
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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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

    override fun create(order: Order): OrderPersistenceResult {
        val key = order.customerId to order.idempotencyKey
        val existing = ordersByKey[key]
        if (existing != null) return OrderPersistenceResult(existing, created = false)
        return OrderPersistenceResult(persist(order).also { ordersByKey[key] = it }, created = true)
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

    @Test
    fun `validates cart and item snapshots before creating`() {
        val useCase = CreateOrderUseCase(CreateOrderRepositoryFake())

        assertFailsWith<com.nexus.shopping.order.application.exception.OrderValidationException> {
            useCase.create(command(items = emptyList()))
        }
        assertFailsWith<com.nexus.shopping.order.application.exception.OrderValidationException> {
            useCase.create(command(items = listOf(item(), item().copy(currency = Currency.USD))))
        }
        assertFailsWith<com.nexus.shopping.order.application.exception.OrderValidationException> {
            useCase.create(command().copy(customerSnapshot = command().customerSnapshot.copy(customerId = 20L)))
        }
        assertFailsWith<com.nexus.shopping.order.application.exception.OrderValidationException> {
            useCase.create(command().copy(idempotencyKey = " "))
        }
    }

    @Test
    fun `validates NUMERIC 12 2 precision while accepting trailing zeros`() {
        val useCase = CreateOrderUseCase(CreateOrderRepositoryFake())

        assertFailsWith<com.nexus.shopping.order.application.exception.OrderValidationException> {
            useCase.create(command(listOf(item().copy(unitPriceAmount = BigDecimal("99999999999")))))
        }
        assertFailsWith<com.nexus.shopping.order.application.exception.OrderValidationException> {
            useCase.create(command(listOf(item().copy(unitPriceAmount = BigDecimal("1E+11")))))
        }

        val created = useCase.create(command(listOf(item().copy(unitPriceAmount = BigDecimal("1.230")))))

        assertEquals(
            BigDecimal("1.230"),
            created.order.items
                .single()
                .unitPriceAmount,
        )
    }

    @Test
    fun `rejects the same idempotency key when cart or any item snapshot field differs`() {
        val useCase = CreateOrderUseCase(CreateOrderRepositoryFake())
        val original = command()
        useCase.create(original)

        val differentPayloads =
            listOf(
                original.copy(cartId = 101L),
                original.copy(items = original.items + item().copy(productId = 2L)),
                original.copy(items = listOf(item().copy(productId = 2L))),
                original.copy(items = listOf(item().copy(productName = "Produto B"))),
                original.copy(items = listOf(item().copy(unitPriceAmount = BigDecimal("20.00")))),
                original.copy(items = listOf(item().copy(currency = Currency.USD))),
                original.copy(items = listOf(item().copy(quantity = 3))),
            )

        differentPayloads.forEach { differentPayload ->
            assertFailsWith<com.nexus.shopping.order.application.exception.OrderIdempotencyConflictException> {
                useCase.create(differentPayload)
            }
        }
    }

    @Test
    fun `concurrent creation returns one creation and replays the persisted order to every loser`() {
        val workers = 8
        val repository = ConcurrentCreateOrderRepositoryFake(workers)
        val useCase = CreateOrderUseCase(repository)
        val executor = Executors.newFixedThreadPool(workers)
        val results = ConcurrentLinkedQueue<Result<com.nexus.shopping.order.application.port.inbound.CreatedOrder>>()
        try {
            val done = CountDownLatch(workers)
            repeat(workers) {
                executor.submit {
                    try {
                        results += runCatching { useCase.create(command()) }
                    } finally {
                        done.countDown()
                    }
                }
            }

            assertTrue(done.await(10, TimeUnit.SECONDS), "Timed out waiting for concurrent order creation")
        } finally {
            executor.shutdownNow()
        }

        assertTrue(results.all { it.isSuccess }, "Expected the uniqueness race to stay behind the repository port: $results")
        val createdOrders = results.map { it.getOrThrow() }
        assertEquals(1, createdOrders.count { !it.replayed })
        assertEquals(7, createdOrders.count { it.replayed })
        assertEquals(1, createdOrders.map { it.order.id }.toSet().size)
    }

    @Test
    fun `concurrent idempotency loser validates the persisted fingerprint before replaying`() {
        val repository =
            ConcurrentCreateOrderRepositoryFake(1) { requested ->
                persistForRace(requested, requestFingerprint = "different-fingerprint")
            }

        assertFailsWith<com.nexus.shopping.order.application.exception.OrderIdempotencyConflictException> {
            CreateOrderUseCase(repository).create(command())
        }
    }
}

private class ConcurrentCreateOrderRepositoryFake(
    participants: Int,
    private val winner: (Order) -> Order = ::persistForRace,
) : OrderRepositoryPort {
    private val initialFinds = CountDownLatch(participants)
    private var persisted: Order? = null

    override fun findById(id: Long): Order? = synchronized(this) { persisted?.takeIf { it.id == id } }

    override fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): Order? {
        initialFinds.countDown()
        assertTrue(initialFinds.await(10, TimeUnit.SECONDS), "Timed out synchronizing initial idempotency lookup")
        return synchronized(this) { persisted }
    }

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Order> = PageResult(emptyList(), page, size, 0, false)

    override fun create(order: Order): OrderPersistenceResult =
        synchronized(this) {
            val existing = persisted
            if (existing != null) {
                OrderPersistenceResult(existing, created = false)
            } else {
                OrderPersistenceResult(winner(order).also { persisted = it }, created = true)
            }
        }

    override fun update(order: Order): Order = order
}

private fun persistForRace(
    order: Order,
    requestFingerprint: String = order.requestFingerprint,
) = Order(
    id = 99L,
    customerId = order.customerId,
    cartId = order.cartId,
    customerSnapshot = order.customerSnapshot,
    shippingAddressSnapshot = order.shippingAddressSnapshot,
    items = order.items,
    status = order.status,
    idempotencyKey = order.idempotencyKey,
    requestFingerprint = requestFingerprint,
    createdAt = Instant.parse("2026-07-26T12:00:00Z"),
    cancelledAt = order.cancelledAt,
)
