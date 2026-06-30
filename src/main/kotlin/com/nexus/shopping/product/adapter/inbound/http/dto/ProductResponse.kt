package com.nexus.shopping.product.adapter.inbound.http.dto

import com.nexus.shopping.product.domain.Product
import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductResponse(
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

fun Product.toResponse() =
    ProductResponse(
        id = id,
        brandId = brandId,
        categoryId = categoryId,
        sku = sku,
        name = name,
        slug = slug,
        description = description,
        status = status.name,
        priceAmount = priceAmount,
        currency = currency.name,
        inventoryQuantity = inventoryQuantity,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
