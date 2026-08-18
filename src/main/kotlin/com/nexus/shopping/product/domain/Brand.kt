package com.nexus.shopping.product.domain

import java.time.LocalDateTime

data class Brand(
    val id: Long?,
    val name: String,
    val description: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
