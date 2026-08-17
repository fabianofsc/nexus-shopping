package com.nexus.shopping.cart.adapter.inbound.http.dto

import com.nexus.shopping.cart.application.command.UpdateCartItemQuantityCommand

data class UpdateCartItemQuantityRequest(
    val quantity: Int,
)

fun UpdateCartItemQuantityRequest.toCommand(
    customerId: Long,
    productId: Long,
): UpdateCartItemQuantityCommand =
    UpdateCartItemQuantityCommand(
        customerId = customerId,
        productId = productId,
        quantity = quantity,
    )
