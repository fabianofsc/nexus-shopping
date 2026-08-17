package com.nexus.shopping.cart.application.command

data class UpdateCartItemQuantityCommand(
    val customerId: Long,
    val productId: Long,
    val quantity: Int,
)
