package com.nexus.shopping.cart.adapter.inbound.http.dto

import com.nexus.shopping.cart.application.command.AddCartItemCommand
import java.math.BigDecimal

data class AddCartItemRequest(
    val productId: Long,
    val productName: String,
    val unitPriceAmount: BigDecimal,
    val currency: String,
    val quantity: Int,
)

fun AddCartItemRequest.toCommand(customerId: Long): AddCartItemCommand =
    AddCartItemCommand(
        customerId = customerId,
        productId = productId,
        productName = productName,
        unitPriceAmount = unitPriceAmount,
        currency = currency,
        quantity = quantity,
    )
