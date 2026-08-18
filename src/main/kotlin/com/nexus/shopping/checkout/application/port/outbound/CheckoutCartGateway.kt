package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.CheckoutCartSnapshot

interface CheckoutCartGateway {
    fun reserveActiveCart(customerId: Long): CheckoutCartSnapshot

    fun confirmCheckout(reservationId: Long)
}
