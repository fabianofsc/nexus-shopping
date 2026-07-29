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
    fun `isolates items from later mutations to the constructor list`() {
        val items = mutableListOf(OrderItemSnapshot(1L, "Produto A", BigDecimal("19.90"), Currency.BRL, 2))
        val order =
            Order(
                id = 1L,
                customerId = 10L,
                cartId = 100L,
                customerSnapshot = CustomerSnapshot(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null),
                shippingAddressSnapshot =
                    ShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
                items = items,
                status = OrderStatus.WAITING_PAYMENT,
                idempotencyKey = "checkout-1",
                requestFingerprint = "fingerprint-1",
                createdAt = null,
                cancelledAt = null,
            )

        items.clear()

        assertEquals(1, order.items.size)
        assertEquals(BigDecimal("39.80"), order.totalAmount)
    }

    @Test
    fun `cancels an order waiting for payment`() {
        assertEquals(OrderStatus.CANCELLED, order().cancel().status)
    }

    @Test
    fun `rejects cancellation from every state except waiting for payment`() {
        listOf(
            OrderStatus.PAYMENT_PROCESSING,
            OrderStatus.PAYMENT_FAILED,
            OrderStatus.CONFIRMED,
            OrderStatus.CANCELLED,
        ).forEach { status ->
            assertFailsWith<OrderStateTransitionException> {
                order(status).cancel()
            }
        }
    }

    @Test
    fun `applies an approved payment result to an order waiting for payment`() {
        val confirmed =
            order().applyPaymentResult(
                attemptReference = "pay_attempt_1",
                result = OrderPaymentResultStatus.APPROVED,
                providerTransactionId = "provider_tx_1",
            )

        assertEquals(OrderStatus.CONFIRMED, confirmed.status)
        assertEquals("pay_attempt_1", confirmed.paymentAttemptReference)
        assertEquals("provider_tx_1", confirmed.paymentProviderTransactionId)
    }

    @Test
    fun `applies a rejected payment result to an order waiting for payment`() {
        val failed =
            order().applyPaymentResult(
                attemptReference = "pay_attempt_2",
                result = OrderPaymentResultStatus.REJECTED,
                providerTransactionId = null,
            )

        assertEquals(OrderStatus.PAYMENT_FAILED, failed.status)
        assertEquals("pay_attempt_2", failed.paymentAttemptReference)
        assertEquals(null, failed.paymentProviderTransactionId)
    }

    @Test
    fun `reapplying the same attempt reference is an idempotent no-op`() {
        val confirmed =
            order().applyPaymentResult(
                attemptReference = "pay_attempt_1",
                result = OrderPaymentResultStatus.APPROVED,
                providerTransactionId = "provider_tx_1",
            )

        val replay =
            confirmed.applyPaymentResult(
                attemptReference = "pay_attempt_1",
                result = OrderPaymentResultStatus.APPROVED,
                providerTransactionId = "provider_tx_1",
            )

        assertEquals(confirmed, replay)
    }

    @Test
    fun `rejects a different payment attempt after a result was applied`() {
        val confirmed =
            order().applyPaymentResult(
                attemptReference = "pay_attempt_1",
                result = OrderPaymentResultStatus.APPROVED,
                providerTransactionId = "provider_tx_1",
            )

        assertFailsWith<OrderStateTransitionException> {
            confirmed.applyPaymentResult(
                attemptReference = "pay_attempt_2",
                result = OrderPaymentResultStatus.REJECTED,
                providerTransactionId = null,
            )
        }
    }
}
