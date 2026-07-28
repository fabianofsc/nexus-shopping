package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderSnapshot
import com.nexus.shopping.integration.checkout.application.model.CreateCheckoutOrderCommand
import com.nexus.shopping.integration.checkout.application.model.FindCheckoutOrderReplayCommand

interface OrderCreationGateway {
    fun findReplay(command: FindCheckoutOrderReplayCommand): CheckoutOrderSnapshot?

    fun create(command: CreateCheckoutOrderCommand): CheckoutOrderSnapshot
}
