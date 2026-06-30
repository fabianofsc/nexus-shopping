package com.nexus.shopping.product.application.port.outbound

import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import java.math.BigDecimal

interface ProductRepositoryPort {
    fun findByCategoryId(categoryId: Long, page: Int, size: Int): ProductPage
    fun findByName(name: String, page: Int, size: Int): ProductPage
    fun save(command: CreateProductCommand): Product
    fun updatePrice(id: Long, priceAmount: BigDecimal): Product?
}
