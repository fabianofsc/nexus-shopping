package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import com.nexus.shopping.product.application.command.CreateCategoryCommand
import com.nexus.shopping.product.application.exception.CategoryValidationException
import com.nexus.shopping.product.application.port.outbound.CategoryRepositoryPort
import com.nexus.shopping.product.domain.Category
import com.nexus.shopping.product.domain.CategoryStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CreateCategoryUseCase(
    private val categoryRepository: CategoryRepositoryPort,
) {
    fun create(command: CreateCategoryCommand): Category {
        logger.infoWithContext("catalog.category.create.started")

        if (command.parentId != null && command.parentId <= 0) {
            throwValidationFailed("parentId must be greater than 0 when provided.")
        }
        if (command.name.isBlank()) throwValidationFailed("name must not be blank.")
        if (command.name.length > 160) throwValidationFailed("name must be at most 160 characters.")
        if (command.slug.isBlank()) throwValidationFailed("slug must not be blank.")
        if (command.slug.length > 180) throwValidationFailed("slug must be at most 180 characters.")
        requireValidStatus(command.status)

        val category = categoryRepository.save(command)
        logger.infoWithContext("catalog.category.create.completed", "category.id" to category.id)
        return category
    }

    private fun requireValidStatus(value: String) {
        val names = CategoryStatus.entries.map { it.name }
        if (value !in names) throwValidationFailed("status must be one of: ${names.joinToString(", ")}.")
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("catalog.category.create.validation_failed", "validation.error" to message)
        throw CategoryValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(CreateCategoryUseCase::class.java)
    }
}
