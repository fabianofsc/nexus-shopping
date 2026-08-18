package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.CheckoutItemSnapshot

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
