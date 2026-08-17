package com.nexus.shopping.inventory.application.port.outbound

interface ProductStockPort {
    fun decrementIfAvailable(
        productId: Long,
        quantity: Int,
    ): Boolean
}
