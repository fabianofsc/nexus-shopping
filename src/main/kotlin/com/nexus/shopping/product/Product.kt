package com.nexus.shopping.product

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
    val status: String,
    val priceAmount: BigDecimal,
    val currency: String,
    val inventoryQuantity: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class ProductPage(
    val content: List<Product>,
    val page: Int,
    val size: Int,
    val count: Int,
    val hasNext: Boolean,
)
