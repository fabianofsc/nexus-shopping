package com.nexus.shopping.product.adapter.inbound.http.dto

import com.nexus.shopping.product.application.command.UpdateProductDetailsCommand

data class UpdateProductDetailsRequest(
    val name: String,
    val slug: String,
    val description: String? = null,
    val brandId: Long,
    val categoryId: Long,
)

fun UpdateProductDetailsRequest.toCommand(id: Long): UpdateProductDetailsCommand =
    UpdateProductDetailsCommand(
        id = id,
        name = name,
        slug = slug,
        description = description,
        brandId = brandId,
        categoryId = categoryId,
    )
