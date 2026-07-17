package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.exception.ProductValidationException
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.ProductPage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProductSearchUseCase(
    private val productRepository: ProductRepositoryPort,
) {
    fun search(
        categoryId: Long?,
        name: String?,
        page: Int,
        size: Int,
    ): ProductPage {
        logger.infoWithContext(
            "product.search.started",
            "product.search.filter" to filterType(categoryId, name),
            "product.search.page" to page,
            "product.search.size" to size,
        )

        if (categoryId != null && !name.isNullOrBlank()) {
            throwValidationFailed("Use either categoryId or name, not both.")
        }
        if (page < 0) {
            throwValidationFailed("Query parameter page must be greater than or equal to 0.")
        }
        if (size !in 1..500) {
            throwValidationFailed("Query parameter size must be between 1 and 500.")
        }

        val result =
            when {
            categoryId != null -> productRepository.findByCategoryId(categoryId, page, size)
            !name.isNullOrBlank() -> productRepository.findByName(name, page, size)
            else -> throwValidationFailed("Query parameter categoryId or name is required.")
        }

        logger.infoWithContext(
            "product.search.completed",
            "product.search.filter" to filterType(categoryId, name),
            "product.search.page" to page,
            "product.search.size" to size,
            "product.search.count" to result.count,
            "product.search.has_next" to result.hasNext,
        )
        return result
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("product.search.validation_failed", "validation.error" to message)
        throw ProductValidationException(message)
    }

    private fun filterType(
        categoryId: Long?,
        name: String?,
    ): String =
        when {
            categoryId != null && !name.isNullOrBlank() -> "multiple"
            categoryId != null -> "category_id"
            !name.isNullOrBlank() -> "name"
            else -> "none"
        }

    private companion object {
        private val logger = LoggerFactory.getLogger(ProductSearchUseCase::class.java)
    }
}
