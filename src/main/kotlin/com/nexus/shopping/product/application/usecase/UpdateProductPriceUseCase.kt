package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import com.nexus.shopping.product.application.command.UpdatePriceCommand
import com.nexus.shopping.product.application.exception.ProductNotFoundException
import com.nexus.shopping.product.application.exception.ProductValidationException
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class UpdateProductPriceUseCase(
    private val productRepository: ProductRepositoryPort,
) {
    fun execute(command: UpdatePriceCommand): Product {
        logger.infoWithContext("product.update_price.started", "product.id" to command.id)
        if (command.priceAmount <= BigDecimal.ZERO) {
            logger.warnWithContext(
                "product.update_price.validation_failed",
                "product.id" to command.id,
                "validation.error" to "priceAmount must be greater than zero.",
            )
            throw ProductValidationException("priceAmount must be greater than zero.")
        }
        val product = productRepository.updatePrice(command.id, command.priceAmount)
        if (product == null) {
            logger.warnWithContext("product.update_price.not_found", "product.id" to command.id)
            throw ProductNotFoundException("Product ${command.id} not found.")
        }

        logger.infoWithContext("product.update_price.completed", "product.id" to command.id)
        return product
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(UpdateProductPriceUseCase::class.java)
    }
}
