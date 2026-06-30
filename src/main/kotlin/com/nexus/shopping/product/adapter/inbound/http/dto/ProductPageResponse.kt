package com.nexus.shopping.product.adapter.inbound.http.dto

data class ProductPageResponse(
    val content: List<ProductResponse>,
    val page: Int,
    val size: Int,
    val count: Int,
    val hasNext: Boolean,
)
