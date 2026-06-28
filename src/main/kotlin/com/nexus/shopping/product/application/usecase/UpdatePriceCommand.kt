package com.nexus.shopping.product.application.usecase

import java.math.BigDecimal

data class UpdatePriceCommand(
    val id: Long,
    val priceAmount: BigDecimal,
)
