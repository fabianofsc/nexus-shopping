package com.nexus.shopping.product.adapter.inbound.http.dto

import java.math.BigDecimal

data class UpdatePriceRequest(
    val priceAmount: BigDecimal,
)
