package com.nexus.shopping.product.adapter.inbound.http.dto

import com.nexus.shopping.product.application.command.CreateCategoryCommand
import com.nexus.shopping.product.application.command.UpdateCategoryStatusCommand
import com.nexus.shopping.product.domain.Category
import java.time.LocalDateTime

data class CreateCategoryRequest(
    val parentId: Long? = null,
    val name: String,
    val slug: String,
    val status: String = "ACTIVE",
)

fun CreateCategoryRequest.toCommand(): CreateCategoryCommand =
    CreateCategoryCommand(
        parentId = parentId,
        name = name,
        slug = slug,
        status = status,
    )

data class UpdateCategoryStatusRequest(
    val status: String,
)

fun UpdateCategoryStatusRequest.toCommand(id: Long): UpdateCategoryStatusCommand =
    UpdateCategoryStatusCommand(
        id = id,
        status = status,
    )

data class CategoryResponse(
    val id: Long,
    val parentId: Long?,
    val name: String,
    val slug: String,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

fun Category.toResponse(): CategoryResponse =
    CategoryResponse(
        id = requireNotNull(id) { "Category.id must be available before mapping to response." },
        parentId = parentId,
        name = name,
        slug = slug,
        status = status.name,
        createdAt = requireNotNull(createdAt) { "Category.createdAt must be available before mapping to response." },
        updatedAt = requireNotNull(updatedAt) { "Category.updatedAt must be available before mapping to response." },
    )
