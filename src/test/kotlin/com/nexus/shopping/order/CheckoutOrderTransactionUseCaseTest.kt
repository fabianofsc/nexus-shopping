package com.nexus.shopping.order

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.inbound.CartCheckoutInputPort
import com.nexus.shopping.cart.application.port.inbound.CartCheckoutReservation
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartItem
import com.nexus.shopping.cart.domain.CartStatus
import com.nexus.shopping.cart.domain.ProductSummary
import com.nexus.shopping.order.application.command.CheckoutOrderCommand
import com.nexus.shopping.order.application.exception.OrderIdempotencyConflictException
import com.nexus.shopping.order.application.exception.OrderValidationException
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.port.outbound.TransactionPort
import com.nexus.shopping.order.application.usecase.CheckoutOrderUseCase
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

private class CheckoutOrderRepositoryFake : OrderRepositoryPort {
    private val ordersById = mutableMapOf<Long, Order>()
    private val ordersByKey = mutableMapOf<Pair<Long, String>, Order>()
    private var nextId = 1L
    var createdOrders = 0
        private set

    override fun findById(id: Long): Order? = ordersById[id]

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Order> = PageResult(emptyList(), page, size, 0, false)

    override fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): Order? = ordersByKey[customerId to idempotencyKey]

    override fun create(order: Order): Order {
        val key = order.customerId to order.idempotencyKey
        return ordersByKey[key] ?: persist(order).also { ordersByKey[key] = it }
    }

    override fun update(order: Order): Order = persist(order)

    private fun persist(order: Order): Order =
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
        ).also {
            ordersById[requireNotNull(it.id)] = it
            if (order.id == null) createdOrders++
        }
}

private class CheckoutCartFake(
    var lockedCart: CartCheckoutReservation?,
) : CartCheckoutInputPort {
    var lockCalls = 0
        private set
    var checkedOutCartId: Long? = null
        private set

    override fun reserveActiveCart(customerId: Long): CartCheckoutReservation {
        lockCalls++
        return lockedCart?.takeIf { it.customerId == customerId }
            ?: throw CartValidationException("customerId $customerId does not have an active cart.")
    }

    override fun confirmCheckout(reservationId: Long) {
        checkedOutCartId = reservationId
        lockedCart = null
    }
}

private object ImmediateTransaction : TransactionPort {
    override fun <T> inTransaction(block: () -> T): T = block()
}

class CheckoutOrderTransactionUseCaseTest {
    private fun command() =
        CheckoutOrderCommand(
            customerId = 10L,
            customerSnapshot = CustomerSnapshot(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null),
            shippingAddressSnapshot =
                ShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
            idempotencyKey = "checkout-1",
        )

    private fun activeCart(items: List<OrderItemSnapshot> = listOf(item())) =
        CartCheckoutReservation(
            Cart(
                id = 100L,
                customerId = 10L,
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

    private fun item() = OrderItemSnapshot(1L, "Produto A", BigDecimal("19.90"), Currency.BRL, 2)

    @Test
    fun `checkout snapshots the locked active cart and marks it checked out in the transaction`() {
        val orders = CheckoutOrderRepositoryFake()
        val carts = CheckoutCartFake(activeCart())

        val order = CheckoutOrderUseCase(orders, carts, ImmediateTransaction).execute(command())

        assertEquals(100L, order.cartId)
        assertEquals(listOf(item()), order.items)
        assertEquals(OrderStatus.WAITING_PAYMENT, order.status)
        assertEquals(100L, carts.checkedOutCartId)
        assertEquals(1, orders.createdOrders)
    }

    @Test
    fun `checkout replays before requiring an active cart`() {
        val orders = CheckoutOrderRepositoryFake()
        val carts = CheckoutCartFake(activeCart())
        val useCase = CheckoutOrderUseCase(orders, carts, ImmediateTransaction)
        val original = useCase.executeWithResult(command())

        val replay = useCase.executeWithResult(command())

        assertEquals(original.order, replay.order)
        assertEquals(false, original.replayed)
        assertEquals(true, replay.replayed)
        assertEquals(1, carts.lockCalls)
        assertEquals(1, orders.createdOrders)
    }

    @Test
    fun `checkout rejects conflicting replay before requiring an active cart`() {
        val orders = CheckoutOrderRepositoryFake()
        val carts = CheckoutCartFake(activeCart())
        val useCase = CheckoutOrderUseCase(orders, carts, ImmediateTransaction)
        useCase.execute(command())

        assertFailsWith<OrderIdempotencyConflictException> {
            useCase.execute(command().copy(shippingAddressSnapshot = command().shippingAddressSnapshot.copy(number = "11")))
        }

        assertEquals(1, carts.lockCalls)
    }

    @Test
    fun `checkout rejects an empty locked cart without creating an order`() {
        val orders = CheckoutOrderRepositoryFake()
        val carts = CheckoutCartFake(activeCart(emptyList()))

        assertFailsWith<OrderValidationException> {
            CheckoutOrderUseCase(orders, carts, ImmediateTransaction).execute(command())
        }

        assertEquals(0, orders.createdOrders)
        assertEquals(null, carts.checkedOutCartId)
    }

    @Test
    fun `checkout validates idempotency key and required snapshot fields before locking the cart`() {
        val orders = CheckoutOrderRepositoryFake()
        val carts = CheckoutCartFake(activeCart())
        val useCase = CheckoutOrderUseCase(orders, carts, ImmediateTransaction)

        assertFailsWith<OrderValidationException> {
            useCase.execute(command().copy(idempotencyKey = "k".repeat(256)))
        }
        assertFailsWith<OrderValidationException> {
            useCase.execute(command().copy(customerSnapshot = command().customerSnapshot.copy(name = " ")))
        }
        assertEquals(0, carts.lockCalls)
        assertEquals(0, orders.createdOrders)
    }

    @Test
    fun `checkout validates snapshot lengths and locked item values before persisting`() {
        val orders = CheckoutOrderRepositoryFake()
        val carts = CheckoutCartFake(activeCart(listOf(item().copy(productName = " "))))
        val useCase = CheckoutOrderUseCase(orders, carts, ImmediateTransaction)

        assertFailsWith<OrderValidationException> {
            useCase.execute(command().copy(shippingAddressSnapshot = command().shippingAddressSnapshot.copy(street = "s".repeat(221))))
        }
        assertFailsWith<OrderValidationException> {
            useCase.execute(command())
        }
        assertEquals(0, orders.createdOrders)
        assertEquals(null, carts.checkedOutCartId)
    }

    @Test
    fun `checkout validates effective NUMERIC 12 2 precision while accepting trailing zeros`() {
        val orders = CheckoutOrderRepositoryFake()
        val carts = CheckoutCartFake(activeCart(listOf(item().copy(unitPriceAmount = BigDecimal("99999999999")))))
        val useCase = CheckoutOrderUseCase(orders, carts, ImmediateTransaction)

        assertFailsWith<OrderValidationException> {
            useCase.execute(command())
        }

        carts.lockedCart = activeCart(listOf(item().copy(unitPriceAmount = BigDecimal("1E+11"))))
        assertFailsWith<OrderValidationException> {
            useCase.execute(command())
        }

        carts.lockedCart = activeCart(listOf(item().copy(unitPriceAmount = BigDecimal("1.230"))))
        val order = useCase.execute(command())

        assertEquals(BigDecimal("1.230"), order.items.single().unitPriceAmount)
    }
}
