package com.nexus.shopping.product.application.command

data class UpdateProductDetailsCommand(
    val id: Long,
    val name: String,
    val slug: String,
    val description: String?,
    val brandId: Long,
    val categoryId: Long,
)
