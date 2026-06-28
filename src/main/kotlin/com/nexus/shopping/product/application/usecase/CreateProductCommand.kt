package com.nexus.shopping.product.application.usecase

import java.math.BigDecimal

data class CreateProductCommand(
    val brandId: Long,
    val categoryId: Long,
    val sku: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val status: String = "ACTIVE",
    val priceAmount: BigDecimal,
    val currency: String = "BRL",
    val inventoryQuantity: Int = 0,
)
