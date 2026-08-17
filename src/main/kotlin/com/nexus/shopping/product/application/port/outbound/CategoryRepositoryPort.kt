package com.nexus.shopping.product.application.port.outbound

import com.nexus.shopping.product.application.command.CreateCategoryCommand
import com.nexus.shopping.product.domain.Category
import com.nexus.shopping.product.domain.CategoryStatus

interface CategoryRepositoryPort {
    fun findById(id: Long): Category?

    fun findAll(): List<Category>

    fun save(command: CreateCategoryCommand): Category

    fun updateStatus(
        id: Long,
        status: CategoryStatus,
    ): Category?
}
