package com.nexus.shopping.product.application.port.outbound

import com.nexus.shopping.product.application.command.CreateBrandCommand
import com.nexus.shopping.product.domain.Brand

interface BrandRepositoryPort {
    fun findById(id: Long): Brand?

    fun findAll(): List<Brand>

    fun save(command: CreateBrandCommand): Brand
}
