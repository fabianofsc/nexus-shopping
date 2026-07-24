package com.nexus.shopping.order.domain

import java.math.BigDecimal
import java.time.Instant

data class Order(
    val id: Long?,
    val customerId: Long,
    val cartId: Long,
    val customerSnapshot: CustomerSnapshot,
    val shippingAddressSnapshot: ShippingAddressSnapshot,
    val items: List<OrderItemSnapshot>,
    val status: OrderStatus,
    val idempotencyKey: String,
    val requestFingerprint: String,
    val createdAt: Instant?,
    val cancelledAt: Instant?,
) {
    val totalAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { total, item -> total + item.totalAmount }

    fun cancel(): Order {
        if (status != OrderStatus.WAITING_PAYMENT) throw OrderStateTransitionException(status)
        return copy(status = OrderStatus.CANCELLED, cancelledAt = Instant.now())
    }
}
