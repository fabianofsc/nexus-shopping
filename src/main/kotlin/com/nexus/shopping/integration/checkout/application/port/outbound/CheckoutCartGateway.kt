package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.CheckoutCartData

interface CheckoutCartGateway {
    fun reserveActiveCart(customerId: Long): CheckoutCartData

    fun confirmCheckout(reservationId: Long)
}
