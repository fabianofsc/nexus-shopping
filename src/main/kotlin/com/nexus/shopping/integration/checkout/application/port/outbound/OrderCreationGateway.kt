package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData
import com.nexus.shopping.integration.checkout.application.model.CreateCheckoutOrderCommand
import com.nexus.shopping.integration.checkout.application.model.FindCheckoutOrderReplayCommand

interface OrderCreationGateway {
    fun findReplay(command: FindCheckoutOrderReplayCommand): CheckoutOrderData?

    fun create(command: CreateCheckoutOrderCommand): CheckoutOrderData
}
