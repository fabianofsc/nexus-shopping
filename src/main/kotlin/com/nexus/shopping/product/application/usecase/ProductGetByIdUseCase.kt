package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.exception.ProductNotFoundException
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProductGetByIdUseCase(
    private val productRepository: ProductRepositoryPort,
) {
    fun execute(id: Long): Product {
        logger.infoWithContext("product.get_by_id.started", "product.id" to id)

        val product = productRepository.findById(id)
        if (product == null) {
            logger.warnWithContext("product.get_by_id.not_found", "product.id" to id)
            throw ProductNotFoundException("Product $id not found.")
        }

        logger.infoWithContext("product.get_by_id.completed", "product.id" to id)
        return product
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ProductGetByIdUseCase::class.java)
    }
}
