package com.nexus.shopping.order.domain

import java.math.BigDecimal
import java.time.Instant

/**
 * O construtor privado obriga a criacao pela factory, que faz uma copia defensiva dos itens.
 * [ConsistentCopyVisibility] da ao [copy] gerado a mesma visibilidade privada, impedindo que chamadas
 * como `order.copy(status = ...)` contornem as transicoes controladas pelo dominio.
 */
@ConsistentCopyVisibility
data class Order private constructor(
    val id: Long?,
    val customerId: Long,
    val cartId: Long,
    val customerSnapshot: CustomerSnapshot,
    val shippingAddressSnapshot: ShippingAddressSnapshot,
    private val itemSnapshots: List<OrderItemSnapshot>,
    val status: OrderStatus,
    val idempotencyKey: String,
    val requestFingerprint: String,
    val createdAt: Instant?,
    val cancelledAt: Instant?,
    val paymentAttemptReference: String?,
    val paymentProviderTransactionId: String?,
) {
    val items: List<OrderItemSnapshot>
        get() = itemSnapshots

    val totalAmount: BigDecimal
        get() = itemSnapshots.fold(BigDecimal.ZERO) { total, item -> total + item.totalAmount }

    fun cancel(): Order {
        if (status != OrderStatus.WAITING_PAYMENT) throw OrderStateTransitionException(status)
        return copy(status = OrderStatus.CANCELLED, cancelledAt = Instant.now())
    }

    fun applyPaymentResult(
        attemptReference: String,
        result: OrderPaymentResultStatus,
        providerTransactionId: String?,
    ): Order {
        if (paymentAttemptReference == attemptReference) return this
        if (status != OrderStatus.WAITING_PAYMENT) throw OrderStateTransitionException(status)

        return copy(
            status =
                when (result) {
                    OrderPaymentResultStatus.APPROVED -> OrderStatus.CONFIRMED
                    OrderPaymentResultStatus.REJECTED -> OrderStatus.PAYMENT_FAILED
                },
            paymentAttemptReference = attemptReference,
            paymentProviderTransactionId = providerTransactionId,
        )
    }

    companion object {
        operator fun invoke(
            id: Long?,
            customerId: Long,
            cartId: Long,
            customerSnapshot: CustomerSnapshot,
            shippingAddressSnapshot: ShippingAddressSnapshot,
            items: List<OrderItemSnapshot>,
            status: OrderStatus,
            idempotencyKey: String,
            requestFingerprint: String,
            createdAt: Instant?,
            cancelledAt: Instant?,
            paymentAttemptReference: String? = null,
            paymentProviderTransactionId: String? = null,
        ): Order =
            Order(
                id = id,
                customerId = customerId,
                cartId = cartId,
                customerSnapshot = customerSnapshot,
                shippingAddressSnapshot = shippingAddressSnapshot,
                itemSnapshots = java.util.List.copyOf(items),
                status = status,
                idempotencyKey = idempotencyKey,
                requestFingerprint = requestFingerprint,
                createdAt = createdAt,
                cancelledAt = cancelledAt,
                paymentAttemptReference = paymentAttemptReference,
                paymentProviderTransactionId = paymentProviderTransactionId,
            )
    }
}
