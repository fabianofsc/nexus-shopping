package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import org.springframework.stereotype.Service

private val VALID_STATUSES = setOf("ACTIVE", "INACTIVE", "ARCHIVED")

@Service
class ProductCreateUseCase(
    private val productRepository: ProductRepositoryPort,
) {

    fun create(command: CreateProductCommand): Product {
        require(command.brandId > 0) { "brandId must be greater than 0." }
        require(command.categoryId > 0) { "categoryId must be greater than 0." }
        require(command.sku.isNotBlank()) { "sku must not be blank." }
        require(command.sku.length <= 120) { "sku must be at most 120 characters." }
        require(command.name.isNotBlank()) { "name must not be blank." }
        require(command.name.length <= 220) { "name must be at most 220 characters." }
        require(command.slug.isNotBlank()) { "slug must not be blank." }
        require(command.slug.length <= 260) { "slug must be at most 260 characters." }
        require(command.description == null || command.description.length <= 2000) {
            "description must be at most 2000 characters."
        }
        require(command.status in VALID_STATUSES) {
            "status must be one of: ${VALID_STATUSES.joinToString(", ")}."
        }
        require(command.priceAmount >= java.math.BigDecimal.ZERO) { "priceAmount must be >= 0." }
        require(command.currency.isNotBlank() && command.currency.length == 3) {
            "currency must be exactly 3 characters."
        }
        require(command.inventoryQuantity >= 0) { "inventoryQuantity must be >= 0." }

        return productRepository.save(command)
    }
}
