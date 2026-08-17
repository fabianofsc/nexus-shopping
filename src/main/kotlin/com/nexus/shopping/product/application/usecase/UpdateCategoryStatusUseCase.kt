package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import com.nexus.shopping.product.application.command.UpdateCategoryStatusCommand
import com.nexus.shopping.product.application.exception.CategoryNotFoundException
import com.nexus.shopping.product.application.exception.CategoryValidationException
import com.nexus.shopping.product.application.port.outbound.CategoryRepositoryPort
import com.nexus.shopping.product.domain.Category
import com.nexus.shopping.product.domain.CategoryStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UpdateCategoryStatusUseCase(
    private val categoryRepository: CategoryRepositoryPort,
) {
    fun execute(command: UpdateCategoryStatusCommand): Category {
        logger.infoWithContext("catalog.category.update_status.started", "category.id" to command.id)

        val names = CategoryStatus.entries.map { it.name }
        if (command.status !in names) {
            logger.warnWithContext(
                "catalog.category.update_status.validation_failed",
                "category.id" to command.id,
                "validation.error" to "status must be one of: ${names.joinToString(", ")}.",
            )
            throw CategoryValidationException("status must be one of: ${names.joinToString(", ")}.")
        }

        val category =
            categoryRepository.updateStatus(command.id, CategoryStatus.valueOf(command.status))
                ?: throw CategoryNotFoundException("Category ${command.id} not found.")

        logger.infoWithContext("catalog.category.update_status.completed", "category.id" to command.id)
        return category
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(UpdateCategoryStatusUseCase::class.java)
    }
}
