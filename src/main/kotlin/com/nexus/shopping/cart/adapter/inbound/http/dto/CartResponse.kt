package com.nexus.shopping.cart.adapter.inbound.http.dto

import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartItem
import java.math.BigDecimal
import java.time.Instant

data class CartResponse(
    val id: Long,
    val customerId: Long,
    val status: String,
    val items: List<CartItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CartItemResponse(
    val productId: Long,
    val productName: String,
    val unitPriceAmount: BigDecimal,
    val currency: String,
    val quantity: Int,
)

fun Cart.toResponse(): CartResponse =
    CartResponse(
        id = requireNotNull(id) { "Cart.id must be available before mapping to response." },
        customerId = customerId,
        status = status.name,
        items = items.map { it.toResponse() },
        createdAt = requireNotNull(createdAt) { "Cart.createdAt must be available before mapping to response." },
        updatedAt = requireNotNull(updatedAt) { "Cart.updatedAt must be available before mapping to response." },
    )

fun CartItem.toResponse(): CartItemResponse =
    CartItemResponse(
        productId = productSummary.productId,
        productName = productSummary.name,
        unitPriceAmount = productSummary.unitPriceAmount,
        currency = productSummary.currency.name,
        quantity = quantity,
    )
