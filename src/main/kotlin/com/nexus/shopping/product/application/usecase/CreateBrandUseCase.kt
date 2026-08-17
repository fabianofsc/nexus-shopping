package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import com.nexus.shopping.product.application.command.CreateBrandCommand
import com.nexus.shopping.product.application.exception.BrandValidationException
import com.nexus.shopping.product.application.port.outbound.BrandRepositoryPort
import com.nexus.shopping.product.domain.Brand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CreateBrandUseCase(
    private val brandRepository: BrandRepositoryPort,
) {
    fun create(command: CreateBrandCommand): Brand {
        logger.infoWithContext("catalog.brand.create.started")

        if (command.name.isBlank()) throwValidationFailed("name must not be blank.")
        if (command.name.length > 160) throwValidationFailed("name must be at most 160 characters.")
        if (command.description != null && command.description.length > 1000) {
            throwValidationFailed("description must be at most 1000 characters.")
        }

        val brand = brandRepository.save(command)
        logger.infoWithContext("catalog.brand.create.completed", "brand.id" to brand.id)
        return brand
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("catalog.brand.create.validation_failed", "validation.error" to message)
        throw BrandValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(CreateBrandUseCase::class.java)
    }
}
