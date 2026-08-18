package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.CheckoutItemSnapshot

interface InventoryGateway {
    fun decrement(
        orderReference: String,
        items: List<CheckoutItemSnapshot>,
    )

    fun release(
        orderReference: String,
        items: List<CheckoutItemSnapshot>,
    )
}
