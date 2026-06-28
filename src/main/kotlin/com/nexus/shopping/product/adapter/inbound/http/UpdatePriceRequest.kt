package com.nexus.shopping.product.adapter.inbound.http

import com.nexus.shopping.product.application.usecase.UpdatePriceCommand
import java.math.BigDecimal

data class UpdatePriceRequest(
    val priceAmount: BigDecimal,
) {
    fun toCommand(id: Long) = UpdatePriceCommand(id = id, priceAmount = priceAmount)
}
