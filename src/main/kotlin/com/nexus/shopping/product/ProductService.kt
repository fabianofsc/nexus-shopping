package com.nexus.shopping.product

import org.springframework.stereotype.Service

@Service
class ProductService(
    private val productRepository: ProductRepository,
) {

    fun findByCategoryId(categoryId: Long, page: Int, size: Int): ProductPage =
        productRepository.findByCategoryId(categoryId, page, size)

    fun findByName(name: String, page: Int, size: Int): ProductPage =
        productRepository.findByName(name, page, size)
}
