package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.CheckoutCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData
import com.nexus.shopping.integration.checkout.application.model.CreateOrderData

interface OrderCreationGateway {
    fun findReplay(command: CheckoutCommand): CheckoutOrderData?

    fun create(data: CreateOrderData): CheckoutOrderData
}
