package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.CheckoutCartSnapshot

interface CheckoutCartGateway {
    fun reserveActiveCart(customerId: Long): CheckoutCartSnapshot

    fun confirmCheckout(reservationId: Long)
}
