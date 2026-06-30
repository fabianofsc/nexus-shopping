package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.command.UpdatePriceCommand
import com.nexus.shopping.product.application.exception.ProductNotFoundException
import com.nexus.shopping.product.application.exception.ProductValidationException
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class UpdateProductPriceUseCase(
    private val productRepository: ProductRepositoryPort,
) {
    fun execute(command: UpdatePriceCommand): Product {
        if (command.priceAmount <= BigDecimal.ZERO) {
            throw ProductValidationException("priceAmount must be greater than zero.")
        }
        return productRepository.updatePrice(command.id, command.priceAmount)
            ?: throw ProductNotFoundException("Product ${command.id} not found.")
    }
}
