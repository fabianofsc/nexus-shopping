package com.nexus.shopping.product.application.command

import java.math.BigDecimal

data class UpdatePriceCommand(
    val id: Long,
    val priceAmount: BigDecimal,
)
