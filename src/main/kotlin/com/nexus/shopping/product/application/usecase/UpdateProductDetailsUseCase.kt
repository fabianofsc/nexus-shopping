package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import com.nexus.shopping.product.application.command.UpdateProductDetailsCommand
import com.nexus.shopping.product.application.exception.ProductNotFoundException
import com.nexus.shopping.product.application.exception.ProductValidationException
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UpdateProductDetailsUseCase(
    private val productRepository: ProductRepositoryPort,
) {
    fun execute(command: UpdateProductDetailsCommand): Product {
        logger.infoWithContext("product.update_details.started", "product.id" to command.id)

        validate(command)

        val product =
            productRepository.updateDetails(command.id, command)
                ?: run {
                    logger.warnWithContext("product.update_details.not_found", "product.id" to command.id)
                    throw ProductNotFoundException("Product ${command.id} not found.")
                }

        logger.infoWithContext("product.update_details.completed", "product.id" to command.id)
        return product
    }

    private fun validate(command: UpdateProductDetailsCommand) {
        if (command.brandId <= 0) throwValidationFailed("brandId must be greater than 0.")
        if (command.categoryId <= 0) throwValidationFailed("categoryId must be greater than 0.")
        if (command.name.isBlank()) throwValidationFailed("name must not be blank.")
        if (command.name.length > 220) throwValidationFailed("name must be at most 220 characters.")
        if (command.slug.isBlank()) throwValidationFailed("slug must not be blank.")
        if (command.slug.length > 260) throwValidationFailed("slug must be at most 260 characters.")
        if (command.description != null && command.description.length > 2000) {
            throwValidationFailed("description must be at most 2000 characters.")
        }
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("product.update_details.validation_failed", "validation.error" to message)
        throw ProductValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(UpdateProductDetailsUseCase::class.java)
    }
}
