package com.nexus.shopping.cart.domain

import java.math.BigDecimal

data class ProductSummary(
    val productId: Long,
    val name: String,
    val unitPriceAmount: BigDecimal,
    val currency: Currency,
)
