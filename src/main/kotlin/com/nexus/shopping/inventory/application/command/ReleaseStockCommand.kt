package com.nexus.shopping.inventory.application.command

data class ReleaseStockCommand(
    val orderReference: String,
    val items: List<ReleaseStockItem>,
)

data class ReleaseStockItem(
    val productId: Long,
    val quantity: Int,
)
