package com.nexus.shopping.cart.application.command

import java.math.BigDecimal

data class AddCartItemCommand(
    val customerId: Long,
    val productId: Long,
    val productName: String,
    val unitPriceAmount: BigDecimal,
    val currency: String,
    val quantity: Int,
)
