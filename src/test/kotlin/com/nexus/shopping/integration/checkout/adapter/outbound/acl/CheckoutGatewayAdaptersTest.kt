package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.cart.application.port.inbound.CartCheckoutInputPort
import com.nexus.shopping.cart.application.port.inbound.CartCheckoutReservation
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartItem
import com.nexus.shopping.cart.domain.CartStatus
import com.nexus.shopping.cart.domain.ProductSummary
import com.nexus.shopping.customer.application.port.inbound.GetCustomerSnapshotInputPort
import com.nexus.shopping.customer.domain.Address
import com.nexus.shopping.customer.domain.Contact
import com.nexus.shopping.customer.domain.Customer
import com.nexus.shopping.customer.domain.CustomerStatus
import com.nexus.shopping.customer.domain.DocumentType
import com.nexus.shopping.integration.checkout.application.exception.CheckoutValidationException
import com.nexus.shopping.integration.checkout.application.model.ApplyOrderPaymentResultByReferenceCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutCustomerSnapshot
import com.nexus.shopping.integration.checkout.application.model.CheckoutItemSnapshot
import com.nexus.shopping.integration.checkout.application.model.CheckoutShippingAddressSnapshot
import com.nexus.shopping.integration.checkout.application.model.CreateCheckoutOrderCommand
import com.nexus.shopping.integration.checkout.application.model.FindCheckoutOrderReplayCommand
import com.nexus.shopping.inventory.application.command.DecrementStockCommand
import com.nexus.shopping.inventory.application.command.DecrementStockItem
import com.nexus.shopping.inventory.application.command.ReleaseStockCommand
import com.nexus.shopping.inventory.application.command.ReleaseStockItem
import com.nexus.shopping.inventory.application.port.inbound.DecrementStockInputPort
import com.nexus.shopping.inventory.application.port.inbound.ReleaseStockInputPort
import com.nexus.shopping.order.application.command.CreateOrderCommand
import com.nexus.shopping.order.application.exception.OrderNotFoundException
import com.nexus.shopping.order.application.port.inbound.ApplyOrderPaymentResultInputPort
import com.nexus.shopping.order.application.port.inbound.CreateOrderInputPort
import com.nexus.shopping.order.application.port.inbound.CreatedOrder
import com.nexus.shopping.order.application.port.inbound.FindOrderReplayCommand
import com.nexus.shopping.order.application.port.inbound.FindOrderReplayInputPort
import com.nexus.shopping.order.application.port.outbound.OrderPersistenceResult
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.usecase.GetOrderByIdUseCase
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.OrderPaymentResultStatus
import com.nexus.shopping.order.domain.OrderStatus
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
import com.nexus.shopping.platform.domain.PageResult
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import com.nexus.shopping.cart.domain.Currency as CartCurrency
import com.nexus.shopping.order.application.command.ApplyOrderPaymentResultCommand as OrderApplyOrderPaymentResultCommand
import com.nexus.shopping.order.domain.Currency as OrderCurrency

class CheckoutGatewayAdaptersTest {
    @Test
    fun `Cart gateway translates reservation snapshot and delegates confirmation`() {
        val reservation =
            CartCheckoutReservation(
                Cart(
                    id = 100L,
                    customerId = 10L,
                    status = CartStatus.ACTIVE,
                    items =
                        listOf(
                            CartItem(
                                ProductSummary(1L, "Produto A", BigDecimal("19.90"), CartCurrency.BRL),
                                quantity = 2,
                            ),
                        ),
                    createdAt = createdAt,
                    updatedAt = createdAt,
                ),
            )
        var confirmedReservationId: Long? = null
        val carts =
            object : CartCheckoutInputPort {
                override fun reserveActiveCart(customerId: Long): CartCheckoutReservation {
                    assertEquals(10L, customerId)
                    return reservation
                }

                override fun confirmCheckout(reservationId: Long) {
                    confirmedReservationId = reservationId
                }
            }
        val gateway = CartCheckoutGatewayAdapter(carts)

        val result = gateway.reserveActiveCart(10L)
        gateway.confirmCheckout(result.reservationId)

        assertEquals(100L, result.reservationId)
        assertEquals(10L, result.customerId)
        assertEquals(listOf(checkoutItem), result.items)
        assertEquals(100L, confirmedReservationId)
    }

    @Test
    fun `Order gateway translates create command and propagates replay metadata`() {
        var capturedCommand: CreateOrderCommand? = null
        val createdOrder = CreatedOrder(order(), replayed = true)
        val orders =
            object : CreateOrderInputPort {
                override fun create(command: CreateOrderCommand): CreatedOrder {
                    capturedCommand = command
                    return createdOrder
                }
            }
        val gateway = OrderCreationGatewayAdapter(orders, NoReplayOrders)

        val result = gateway.create(createOrderCommand)

        assertEquals(expectedCreateOrderCommand, capturedCommand)
        assertEquals(1L, result.id)
        assertEquals("checkout:1", result.orderReference)
        assertEquals(100L, result.cartId)
        assertEquals("ana@example.com", result.recipientEmail)
        assertEquals(listOf(checkoutItem), result.items)
        assertEquals(BigDecimal("39.80"), result.totalAmount)
        assertEquals("WAITING_PAYMENT", result.status)
        assertEquals(true, result.replayed)
    }

    @Test
    fun `Order gateway translates replay lookup and returns the original order`() {
        var capturedCommand: FindOrderReplayCommand? = null
        val createdOrder = CreatedOrder(order(), replayed = true)
        val replayOrders =
            object : FindOrderReplayInputPort {
                override fun findReplay(command: FindOrderReplayCommand): CreatedOrder {
                    capturedCommand = command
                    return createdOrder
                }
            }
        val gateway =
            OrderCreationGatewayAdapter(
                orders =
                    object : CreateOrderInputPort {
                        override fun create(command: CreateOrderCommand): CreatedOrder = error("Not used")
                    },
                replayOrders = replayOrders,
            )

        val result = gateway.findReplay(findOrderReplayCommand)

        assertEquals(expectedReplayCommand, capturedCommand)
        assertEquals(true, result?.replayed)
        assertEquals(1L, result?.id)
        assertEquals("checkout:1", result?.orderReference)
        assertEquals("ana@example.com", result?.recipientEmail)
        assertEquals(listOf(checkoutItem), result?.items)
    }

    @Test
    fun `Order payment result gateway resolves the order id from the checkout reference and reports a real transition`() {
        val before = order()
        val after = before.applyPaymentResult("pay-77", OrderPaymentResultStatus.APPROVED, "provider-tx-1")
        var capturedCommand: OrderApplyOrderPaymentResultCommand? = null
        val orders =
            object : ApplyOrderPaymentResultInputPort {
                override fun apply(command: OrderApplyOrderPaymentResultCommand): Order {
                    capturedCommand = command
                    return after
                }
            }
        val gateway = OrderPaymentResultGatewayAdapter(orders, GetOrderByIdUseCase(orderRepository(before)))

        val result =
            gateway.applyByOrderReference(
                ApplyOrderPaymentResultByReferenceCommand(
                    orderReference = "checkout:1",
                    attemptReference = "pay-77",
                    status = "APPROVED",
                    providerTransactionId = "provider-tx-1",
                ),
            )

        assertEquals(1L, capturedCommand?.orderId)
        assertEquals("pay-77", capturedCommand?.attemptReference)
        assertEquals("APPROVED", capturedCommand?.status)
        assertEquals("provider-tx-1", capturedCommand?.providerTransactionId)
        assertEquals(1L, result.orderId)
        assertEquals(10L, result.customerId)
        assertEquals("ana@example.com", result.recipientEmail)
        assertEquals("CONFIRMED", result.status)
        assertEquals(true, result.transitioned)
    }

    @Test
    fun `Order payment result gateway reports no transition when the order was already terminal`() {
        val alreadyTerminal = order().applyPaymentResult("pay-old", OrderPaymentResultStatus.APPROVED, "provider-old")
        val orders =
            object : ApplyOrderPaymentResultInputPort {
                override fun apply(command: OrderApplyOrderPaymentResultCommand): Order = alreadyTerminal
            }
        val gateway = OrderPaymentResultGatewayAdapter(orders, GetOrderByIdUseCase(orderRepository(alreadyTerminal)))

        val result =
            gateway.applyByOrderReference(
                ApplyOrderPaymentResultByReferenceCommand(
                    orderReference = "checkout:1",
                    attemptReference = "pay-new",
                    status = "APPROVED",
                    providerTransactionId = "provider-new",
                ),
            )

        assertEquals(false, result.transitioned)
    }

    @Test
    fun `Order payment result gateway rejects a reference outside the checkout format`() {
        val orders =
            object : ApplyOrderPaymentResultInputPort {
                override fun apply(command: OrderApplyOrderPaymentResultCommand): Order = error("Not used")
            }
        val gateway = OrderPaymentResultGatewayAdapter(orders, GetOrderByIdUseCase(orderRepository(order())))

        assertFailsWith<OrderNotFoundException> {
            gateway.applyByOrderReference(
                ApplyOrderPaymentResultByReferenceCommand(
                    orderReference = "not-a-checkout-reference",
                    attemptReference = "pay-1",
                    status = "APPROVED",
                    providerTransactionId = null,
                ),
            )
        }
    }

    private fun orderRepository(existing: Order) =
        object : OrderRepositoryPort {
            override fun findById(id: Long): Order? = if (id == existing.id) existing else null

            override fun findByCustomerIdAndIdempotencyKey(
                customerId: Long,
                idempotencyKey: String,
            ): Order? = error("Not used")

            override fun findByCustomerId(
                customerId: Long,
                page: Int,
                size: Int,
            ): PageResult<Order> = error("Not used")

            override fun create(order: Order): OrderPersistenceResult = error("Not used")

            override fun update(order: Order): Order = error("Not used")
        }

    @Test
    fun `Inventory gateway translates checkout items into decrement and release commands`() {
        val decrements = mutableListOf<DecrementStockCommand>()
        val releases = mutableListOf<ReleaseStockCommand>()
        val decrementPort =
            object : DecrementStockInputPort {
                override fun decrement(command: DecrementStockCommand) {
                    decrements += command
                }
            }
        val releasePort =
            object : ReleaseStockInputPort {
                override fun release(command: ReleaseStockCommand) {
                    releases += command
                }
            }
        val gateway = InventoryGatewayAdapter(decrementPort, releasePort)
        val items = listOf(checkoutItem)

        gateway.decrement("checkout:1", items)
        gateway.release("checkout:1", items)

        assertEquals(DecrementStockCommand("checkout:1", listOf(DecrementStockItem(1L, 2))), decrements.single())
        assertEquals(ReleaseStockCommand("checkout:1", listOf(ReleaseStockItem(1L, 2))), releases.single())
    }

    @Test
    fun `Customer gateway resolves customer and address snapshots from the registered customer`() {
        val customer =
            Customer(
                id = 10L,
                name = "Ana Silva",
                document = "12345678900",
                documentType = DocumentType.CPF,
                status = CustomerStatus.ACTIVE,
                contact = Contact("ana@example.com", "+5511999990000"),
                address = Address("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
                createdAt = java.time.LocalDateTime.of(2026, 7, 26, 12, 0),
                updatedAt = java.time.LocalDateTime.of(2026, 7, 26, 12, 0),
            )
        val snapshotPort =
            object : GetCustomerSnapshotInputPort {
                override fun getSnapshot(customerId: Long): Customer? = if (customerId == 10L) customer else null
            }
        val gateway = CustomerCheckoutGatewayAdapter(snapshotPort)

        val resolution = gateway.resolve(10L)

        assertEquals(10L, resolution.customer.customerId)
        assertEquals("Ana Silva", resolution.customer.name)
        assertEquals("ana@example.com", resolution.customer.email)
        assertEquals("Sao Paulo", resolution.shippingAddress.city)
        assertEquals("Rua A", resolution.shippingAddress.street)
    }

    @Test
    fun `Customer gateway throws CheckoutValidationException when customer does not exist`() {
        val gateway =
            CustomerCheckoutGatewayAdapter(
                object : GetCustomerSnapshotInputPort {
                    override fun getSnapshot(customerId: Long): Customer? = null
                },
            )

        assertFailsWith<CheckoutValidationException> {
            gateway.resolve(999L)
        }
    }

    private fun order() =
        Order(
            id = 1L,
            customerId = 10L,
            cartId = 100L,
            customerSnapshot = orderCustomer,
            shippingAddressSnapshot = orderShippingAddress,
            items = listOf(orderItem),
            status = OrderStatus.WAITING_PAYMENT,
            idempotencyKey = "checkout-1",
            requestFingerprint = "fingerprint",
            createdAt = createdAt,
            cancelledAt = null,
        )

    private companion object {
        val createdAt: Instant = Instant.parse("2026-07-26T12:00:00Z")
        val checkoutCustomer = CheckoutCustomerSnapshot(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null)
        val checkoutShippingAddress =
            CheckoutShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR")
        val checkoutItem = CheckoutItemSnapshot(1L, "Produto A", BigDecimal("19.90"), "BRL", 2)
        val findOrderReplayCommand =
            FindCheckoutOrderReplayCommand(
                customerId = 10L,
                customerSnapshot = checkoutCustomer,
                shippingAddressSnapshot = checkoutShippingAddress,
                idempotencyKey = "checkout-1",
                paymentAuthorizationFingerprint = "opaque-payment-authorization-fingerprint",
            )
        val createOrderCommand =
            CreateCheckoutOrderCommand(
                customerId = 10L,
                cartId = 100L,
                customerSnapshot = checkoutCustomer,
                shippingAddressSnapshot = checkoutShippingAddress,
                items = listOf(checkoutItem),
                idempotencyKey = "checkout-1",
                paymentAuthorizationFingerprint = "opaque-payment-authorization-fingerprint",
            )
        val orderCustomer = CustomerSnapshot(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null)
        val orderShippingAddress =
            ShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR")
        val orderItem = OrderItemSnapshot(1L, "Produto A", BigDecimal("19.90"), OrderCurrency.BRL, 2)
        val expectedCreateOrderCommand =
            CreateOrderCommand(
                customerId = 10L,
                cartId = 100L,
                customerSnapshot = orderCustomer,
                shippingAddressSnapshot = orderShippingAddress,
                items = listOf(orderItem),
                idempotencyKey = "checkout-1",
                paymentAuthorizationFingerprint = "opaque-payment-authorization-fingerprint",
            )
        val expectedReplayCommand =
            FindOrderReplayCommand(
                customerId = 10L,
                customerSnapshot = orderCustomer,
                shippingAddressSnapshot = orderShippingAddress,
                idempotencyKey = "checkout-1",
                paymentAuthorizationFingerprint = "opaque-payment-authorization-fingerprint",
            )

        object NoReplayOrders : FindOrderReplayInputPort {
            override fun findReplay(command: FindOrderReplayCommand): CreatedOrder? = null
        }
    }
}
