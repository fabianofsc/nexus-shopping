package com.nexus.shopping.order.domain

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrderTest {
    private fun order(status: OrderStatus = OrderStatus.WAITING_PAYMENT) =
        Order(
            id = 1L,
            customerId = 10L,
            cartId = 100L,
            customerSnapshot = CustomerSnapshot(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null),
            shippingAddressSnapshot =
                ShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
            items =
                listOf(
                    OrderItemSnapshot(1L, "Produto A", BigDecimal("19.90"), Currency.BRL, 2),
                    OrderItemSnapshot(2L, "Produto B", BigDecimal("5.00"), Currency.BRL, 3),
                ),
            status = status,
            idempotencyKey = "checkout-1",
            requestFingerprint = "fingerprint-1",
            createdAt = null,
            cancelledAt = null,
        )

    @Test
    fun `derives the order total from immutable item snapshots`() {
        assertEquals(BigDecimal("54.80"), order().totalAmount)
    }

    @Test
    fun `cancels an order waiting for payment`() {
        assertEquals(OrderStatus.CANCELLED, order().cancel().status)
    }

    @Test
    fun `rejects cancellation from every state except waiting for payment`() {
        assertFailsWith<OrderStateTransitionException> {
            order(OrderStatus.PAYMENT_PROCESSING).cancel()
        }
    }
}
