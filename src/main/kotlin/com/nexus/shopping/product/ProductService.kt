package com.nexus.shopping.product

import org.springframework.stereotype.Service

@Service
class ProductService(
    private val productRepository: ProductRepository,
) {

    fun findByCategoryId(categoryId: Long): List<Product> =
        productRepository.findByCategoryId(categoryId)

    fun findByName(name: String): List<Product> =
        productRepository.findByName(name)
}
