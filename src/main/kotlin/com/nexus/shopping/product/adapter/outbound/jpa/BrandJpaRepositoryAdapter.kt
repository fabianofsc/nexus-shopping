package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.command.CreateBrandCommand
import com.nexus.shopping.product.application.port.outbound.BrandRepositoryPort
import com.nexus.shopping.product.domain.Brand
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class BrandJpaRepositoryAdapter(
    private val repository: SpringDataBrandRepository,
) : BrandRepositoryPort {
    @Transactional(readOnly = true)
    override fun findById(id: Long): Brand? = repository.findById(id).orElse(null)?.toDomain()

    @Transactional(readOnly = true)
    override fun findAll(): List<Brand> = repository.findAll().map { it.toDomain() }

    @Transactional
    override fun save(command: CreateBrandCommand): Brand = repository.saveAndFlush(command.toEntity()).toDomain()
}
