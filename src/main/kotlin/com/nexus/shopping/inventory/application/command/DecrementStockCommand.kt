package com.nexus.shopping.inventory.application.command

data class DecrementStockCommand(
    val orderReference: String,
    val items: List<DecrementStockItem>,
)

data class DecrementStockItem(
    val productId: Long,
    val quantity: Int,
)
