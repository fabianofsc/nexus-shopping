package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.product.application.port.outbound.BrandRepositoryPort
import com.nexus.shopping.product.domain.Brand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ListBrandsUseCase(
    private val brandRepository: BrandRepositoryPort,
) {
    fun list(): List<Brand> {
        logger.infoWithContext("catalog.brand.list.started")
        return brandRepository.findAll()
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ListBrandsUseCase::class.java)
    }
}
