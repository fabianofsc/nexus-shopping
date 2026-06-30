package com.nexus.shopping.product.adapter.inbound.http.dto

import com.nexus.shopping.product.domain.ProductPage

data class ProductPageResponse(
    val content: List<ProductResponse>,
    val page: Int,
    val size: Int,
    val count: Int,
    val hasNext: Boolean,
)

fun ProductPage.toResponse() =
    ProductPageResponse(
        content = content.map { it.toResponse() },
        page = page,
        size = size,
        count = count,
        hasNext = hasNext,
    )
