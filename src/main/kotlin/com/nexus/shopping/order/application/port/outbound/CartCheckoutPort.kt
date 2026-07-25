package com.nexus.shopping.order.application.port.outbound

import com.nexus.shopping.order.domain.OrderItemSnapshot

/**
 * Provides the checkout-only view of a cart. The returned cart and its items are read while the
 * adapter holds a write lock; they are deliberately represented by Order snapshot types, never by
 * Cart or JPA entities.
 */
interface CartCheckoutPort {
    fun lockActiveCartByCustomerId(customerId: Long): CheckoutCartSnapshot?

    fun markCheckedOut(cartId: Long)
}

data class CheckoutCartSnapshot(
    val cartId: Long,
    val customerId: Long,
    val items: List<OrderItemSnapshot>,
)
