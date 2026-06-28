package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.ProductPage
import org.springframework.stereotype.Service

@Service
class ProductSearchUseCase(
    private val productRepository: ProductRepositoryPort,
) {

    fun search(categoryId: Long?, name: String?, page: Int, size: Int): ProductPage {
        if (categoryId != null && !name.isNullOrBlank()) {
            throw ProductValidationException("Use either categoryId or name, not both.")
        }
        if (page < 0) {
            throw ProductValidationException("Query parameter page must be greater than or equal to 0.")
        }
        if (size !in 1..500) {
            throw ProductValidationException("Query parameter size must be between 1 and 500.")
        }

        return when {
            categoryId != null -> productRepository.findByCategoryId(categoryId, page, size)
            !name.isNullOrBlank() -> productRepository.findByName(name, page, size)
            else -> throw ProductValidationException("Query parameter categoryId or name is required.")
        }
    }
}
