package com.nexus.shopping.product.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class Product(
    val id: Long,
    val brandId: Long,
    val categoryId: Long,
    val sku: String,
    val name: String,
    val slug: String,
    val description: String?,
    val status: ProductStatus,
    val priceAmount: BigDecimal,
    val currency: Currency,
    val inventoryQuantity: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
