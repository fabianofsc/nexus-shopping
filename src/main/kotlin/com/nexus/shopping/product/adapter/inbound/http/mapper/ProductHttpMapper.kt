package com.nexus.shopping.product.adapter.inbound.http.mapper

import com.nexus.shopping.product.adapter.inbound.http.dto.CreateProductRequest
import com.nexus.shopping.product.adapter.inbound.http.dto.ProductPageResponse
import com.nexus.shopping.product.adapter.inbound.http.dto.ProductResponse
import com.nexus.shopping.product.adapter.inbound.http.dto.UpdatePriceRequest
import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.application.command.UpdatePriceCommand
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage

fun CreateProductRequest.toCommand() = CreateProductCommand(
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

fun UpdatePriceRequest.toCommand(id: Long) = UpdatePriceCommand(id = id, priceAmount = priceAmount)

fun Product.toResponse() = ProductResponse(
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

fun ProductPage.toResponse() = ProductPageResponse(
    content = content.map { it.toResponse() },
    page = page,
    size = size,
    count = count,
    hasNext = hasNext,
)
