package com.nexus.shopping.product.application.port.outbound

import com.nexus.shopping.platform.domain.PageResult
import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.domain.Product
import java.math.BigDecimal

interface ProductRepositoryPort {
    fun findById(id: Long): Product?

    fun findByCategoryId(
        categoryId: Long,
        page: Int,
        size: Int,
    ): PageResult<Product>

    fun findByName(
        name: String,
        page: Int,
        size: Int,
    ): PageResult<Product>

    fun save(command: CreateProductCommand): Product

    fun updatePrice(
        id: Long,
        priceAmount: BigDecimal,
    ): Product?
}
