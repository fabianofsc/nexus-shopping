package com.nexus.shopping.product.adapter.inbound.http.dto

import java.math.BigDecimal

data class CreateProductRequest(
    val brandId: Long,
    val categoryId: Long,
    val sku: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val status: String? = null,
    val priceAmount: BigDecimal,
    val currency: String? = null,
    val inventoryQuantity: Int? = null,
)
