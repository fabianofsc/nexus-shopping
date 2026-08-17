package com.nexus.shopping.product.domain

import java.time.LocalDateTime

data class Category(
    val id: Long?,
    val parentId: Long?,
    val name: String,
    val slug: String,
    val status: CategoryStatus,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
