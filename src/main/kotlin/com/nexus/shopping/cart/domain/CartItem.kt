package com.nexus.shopping.cart.domain

data class CartItem(
    val productSummary: ProductSummary,
    val quantity: Int,
)
