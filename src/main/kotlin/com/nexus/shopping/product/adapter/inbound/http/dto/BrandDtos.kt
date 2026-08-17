package com.nexus.shopping.product.adapter.inbound.http.dto

import com.nexus.shopping.product.application.command.CreateBrandCommand
import com.nexus.shopping.product.domain.Brand
import java.time.LocalDateTime

data class CreateBrandRequest(
    val name: String,
    val description: String? = null,
)

fun CreateBrandRequest.toCommand(): CreateBrandCommand = CreateBrandCommand(name = name, description = description)

data class BrandResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

fun Brand.toResponse(): BrandResponse =
    BrandResponse(
        id = requireNotNull(id) { "Brand.id must be available before mapping to response." },
        name = name,
        description = description,
        createdAt = requireNotNull(createdAt) { "Brand.createdAt must be available before mapping to response." },
        updatedAt = requireNotNull(updatedAt) { "Brand.updatedAt must be available before mapping to response." },
    )
