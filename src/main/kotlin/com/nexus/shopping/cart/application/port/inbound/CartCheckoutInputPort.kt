package com.nexus.shopping.cart.application.port.inbound

import com.nexus.shopping.cart.domain.Cart

interface CartCheckoutInputPort {
    fun reserveActiveCart(customerId: Long): CartCheckoutReservation

    fun confirmCheckout(reservationId: Long)
}

data class CartCheckoutReservation(
    val cart: Cart,
) {
    val id: Long
        get() = requireNotNull(cart.id) { "CartCheckoutReservation requires a persisted cart." }

    val customerId: Long
        get() = cart.customerId

    val items
        get() = cart.items
}
