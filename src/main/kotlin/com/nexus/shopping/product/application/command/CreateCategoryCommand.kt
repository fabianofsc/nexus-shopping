package com.nexus.shopping.product.application.command

data class CreateCategoryCommand(
    val parentId: Long?,
    val name: String,
    val slug: String,
    val status: String,
)
