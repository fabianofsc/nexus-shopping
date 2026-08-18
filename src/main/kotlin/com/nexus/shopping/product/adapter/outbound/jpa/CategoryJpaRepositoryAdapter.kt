package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.command.CreateCategoryCommand
import com.nexus.shopping.product.application.port.outbound.CategoryRepositoryPort
import com.nexus.shopping.product.domain.Category
import com.nexus.shopping.product.domain.CategoryStatus
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class CategoryJpaRepositoryAdapter(
    private val repository: SpringDataCategoryRepository,
) : CategoryRepositoryPort {
    @Transactional(readOnly = true)
    override fun findById(id: Long): Category? = repository.findById(id).orElse(null)?.toDomain()

    @Transactional(readOnly = true)
    override fun findAll(): List<Category> = repository.findAll().map { it.toDomain() }

    @Transactional
    override fun save(command: CreateCategoryCommand): Category = repository.saveAndFlush(command.toEntity()).toDomain()

    @Transactional
    @CacheEvict(cacheNames = [ProductCacheConfig.PRODUCT_SEARCH_CACHE], allEntries = true)
    override fun updateStatus(
        id: Long,
        status: CategoryStatus,
    ): Category? {
        val entity = repository.findById(id).orElse(null) ?: return null
        entity.status = status
        return repository.saveAndFlush(entity).toDomain()
    }
}
