package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.product.application.port.outbound.CategoryRepositoryPort
import com.nexus.shopping.product.domain.Category
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ListCategoriesUseCase(
    private val categoryRepository: CategoryRepositoryPort,
) {
    fun list(): List<Category> {
        logger.infoWithContext("catalog.category.list.started")
        return categoryRepository.findAll()
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ListCategoriesUseCase::class.java)
    }
}
