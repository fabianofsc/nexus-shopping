package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Currency
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductStatus
import com.nexus.shopping.shared.requireValidEnum
import org.springframework.stereotype.Service

@Service
class ProductCreateUseCase(
    private val productRepository: ProductRepositoryPort,
) {

    fun create(command: CreateProductCommand): Product {
        if (command.brandId <= 0) throw ProductValidationException("brandId must be greater than 0.")
        if (command.categoryId <= 0) throw ProductValidationException("categoryId must be greater than 0.")
        if (command.sku.isBlank()) throw ProductValidationException("sku must not be blank.")
        if (command.sku.length > 120) throw ProductValidationException("sku must be at most 120 characters.")
        if (command.name.isBlank()) throw ProductValidationException("name must not be blank.")
        if (command.name.length > 220) throw ProductValidationException("name must be at most 220 characters.")
        if (command.slug.isBlank()) throw ProductValidationException("slug must not be blank.")
        if (command.slug.length > 260) throw ProductValidationException("slug must be at most 260 characters.")
        if (command.description != null && command.description.length > 2000) {
            throw ProductValidationException("description must be at most 2000 characters.")
        }
        requireValidEnum<ProductStatus>(command.status, "status") { ProductValidationException(it) }
        if (command.priceAmount < java.math.BigDecimal.ZERO) throw ProductValidationException("priceAmount must be >= 0.")
        requireValidEnum<Currency>(command.currency, "currency") { ProductValidationException(it) }
        if (command.inventoryQuantity < 0) throw ProductValidationException("inventoryQuantity must be >= 0.")

        return productRepository.save(command)
    }
}
