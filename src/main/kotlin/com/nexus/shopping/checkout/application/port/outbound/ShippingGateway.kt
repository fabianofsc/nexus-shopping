package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.CheckoutShippingCommand

interface ShippingGateway {
    fun process(command: CheckoutShippingCommand)
}
