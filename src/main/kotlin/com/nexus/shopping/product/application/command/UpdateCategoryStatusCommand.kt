package com.nexus.shopping.product.application.command

data class UpdateCategoryStatusCommand(
    val id: Long,
    val status: String,
)
