package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData
import com.nexus.shopping.integration.checkout.application.model.CreateOrderData
import com.nexus.shopping.integration.checkout.application.model.FindOrderReplayData

interface OrderCreationGateway {
    fun findReplay(data: FindOrderReplayData): CheckoutOrderData?

    fun create(data: CreateOrderData): CheckoutOrderData
}
