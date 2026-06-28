package com.nexus.shopping.product.adapter.inbound.http

import com.nexus.shopping.product.application.usecase.CreateProductCommand
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
) {
    fun toCommand() = CreateProductCommand(
        brandId = brandId,
        categoryId = categoryId,
        sku = sku,
        name = name,
        slug = slug,
        description = description,
        status = status ?: "ACTIVE",
        priceAmount = priceAmount,
        currency = currency ?: "BRL",
        inventoryQuantity = inventoryQuantity ?: 0,
    )
}
