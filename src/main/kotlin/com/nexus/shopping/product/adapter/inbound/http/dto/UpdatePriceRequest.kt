package com.nexus.shopping.product.adapter.inbound.http.dto

import com.nexus.shopping.product.application.command.UpdatePriceCommand
import java.math.BigDecimal

data class UpdatePriceRequest(
    val priceAmount: BigDecimal,
)

fun UpdatePriceRequest.toCommand(id: Long) = UpdatePriceCommand(id = id, priceAmount = priceAmount)
