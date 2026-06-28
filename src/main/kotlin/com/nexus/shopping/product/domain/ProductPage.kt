package com.nexus.shopping.product.domain

data class ProductPage(
    val content: List<Product>,
    val page: Int,
    val size: Int,
    val count: Int,
    val hasNext: Boolean,
)
