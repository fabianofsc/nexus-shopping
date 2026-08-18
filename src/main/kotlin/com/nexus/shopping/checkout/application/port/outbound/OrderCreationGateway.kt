package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.CheckoutOrderSnapshot
import com.nexus.shopping.checkout.application.model.CreateCheckoutOrderCommand
import com.nexus.shopping.checkout.application.model.FindCheckoutOrderReplayCommand

interface OrderCreationGateway {
    fun findReplay(command: FindCheckoutOrderReplayCommand): CheckoutOrderSnapshot?

    fun create(command: CreateCheckoutOrderCommand): CheckoutOrderSnapshot
}
