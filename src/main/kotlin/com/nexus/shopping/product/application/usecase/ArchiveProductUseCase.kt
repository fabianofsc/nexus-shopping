package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import com.nexus.shopping.product.application.exception.ProductNotFoundException
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ArchiveProductUseCase(
    private val productRepository: ProductRepositoryPort,
) {
    fun execute(id: Long): Product {
        logger.infoWithContext("product.archive.started", "product.id" to id)

        val product =
            productRepository.archive(id)
                ?: run {
                    logger.warnWithContext("product.archive.not_found", "product.id" to id)
                    throw ProductNotFoundException("Product $id not found.")
                }

        logger.infoWithContext("product.archive.completed", "product.id" to id)
        return product
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ArchiveProductUseCase::class.java)
    }
}
