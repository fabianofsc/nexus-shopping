package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.cart.application.port.inbound.CartCheckoutInputPort
import com.nexus.shopping.cart.application.port.inbound.CartCheckoutReservation
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartItem
import com.nexus.shopping.cart.domain.CartStatus
import com.nexus.shopping.cart.domain.ProductSummary
import com.nexus.shopping.integration.checkout.application.model.CheckoutCustomerData
import com.nexus.shopping.integration.checkout.application.model.CheckoutItemData
import com.nexus.shopping.integration.checkout.application.model.CheckoutShippingAddressData
import com.nexus.shopping.integration.checkout.application.model.CreateCheckoutOrderCommand
import com.nexus.shopping.integration.checkout.application.model.FindCheckoutOrderReplayCommand
import com.nexus.shopping.order.application.command.CreateOrderCommand
import com.nexus.shopping.order.application.port.inbound.CreateOrderInputPort
import com.nexus.shopping.order.application.port.inbound.CreatedOrder
import com.nexus.shopping.order.application.port.inbound.FindOrderReplayCommand
import com.nexus.shopping.order.application.port.inbound.FindOrderReplayInputPort
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.OrderStatus
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import com.nexus.shopping.cart.domain.Currency as CartCurrency
import com.nexus.shopping.order.domain.Currency as OrderCurrency

class CheckoutGatewayAdaptersTest {
    @Test
    fun `Cart gateway translates reservation data and delegates confirmation`() {
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
    fun `Order gateway translates create data and propagates replay metadata`() {
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

        val result = gateway.create(createOrderData)

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

        val result = gateway.findReplay(findOrderReplayData)

        assertEquals(expectedReplayCommand, capturedCommand)
        assertEquals(true, result?.replayed)
        assertEquals(1L, result?.id)
        assertEquals("checkout:1", result?.orderReference)
        assertEquals("ana@example.com", result?.recipientEmail)
        assertEquals(listOf(checkoutItem), result?.items)
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
        val checkoutCustomer = CheckoutCustomerData(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null)
        val checkoutShippingAddress =
            CheckoutShippingAddressData("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR")
        val checkoutItem = CheckoutItemData(1L, "Produto A", BigDecimal("19.90"), "BRL", 2)
        val findOrderReplayData =
            FindCheckoutOrderReplayCommand(
                customerId = 10L,
                customerSnapshot = checkoutCustomer,
                shippingAddressSnapshot = checkoutShippingAddress,
                idempotencyKey = "checkout-1",
                paymentAuthorizationFingerprint = "opaque-payment-authorization-fingerprint",
            )
        val createOrderData =
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
