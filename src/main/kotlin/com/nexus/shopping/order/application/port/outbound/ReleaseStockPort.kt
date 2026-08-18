package com.nexus.shopping.order.application.port.outbound

interface ReleaseStockPort {
    fun release(
        orderReference: String,
        items: List<ReleasedStockItem>,
    )
}

data class ReleasedStockItem(
    val productId: Long,
    val quantity: Int,
)
