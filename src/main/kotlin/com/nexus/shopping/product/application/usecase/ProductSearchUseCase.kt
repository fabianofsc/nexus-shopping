package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.ProductPage
import org.springframework.stereotype.Service

@Service
class ProductSearchUseCase(
    private val productRepository: ProductRepositoryPort,
) {

    fun search(categoryId: Long?, name: String?, page: Int, size: Int): ProductPage {
        require(!(categoryId != null && !name.isNullOrBlank())) {
            "Use either categoryId or name, not both."
        }
        require(page >= 0) {
            "Query parameter page must be greater than or equal to 0."
        }
        require(size in 1..500) {
            "Query parameter size must be between 1 and 500."
        }

        return when {
            categoryId != null -> productRepository.findByCategoryId(categoryId, page, size)
            !name.isNullOrBlank() -> productRepository.findByName(name, page, size)
            else -> throw IllegalArgumentException("Query parameter categoryId or name is required.")
        }
    }
}
