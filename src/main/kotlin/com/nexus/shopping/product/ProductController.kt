package com.nexus.shopping.product

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/products")
class ProductController(
    private val productService: ProductService,
) {

    @GetMapping
    fun search(
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) name: String?,
    ): List<Product> {
        if (categoryId != null && !name.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Use either categoryId or name, not both.")
        }

        if (categoryId != null) {
            return productService.findByCategoryId(categoryId)
        }

        if (!name.isNullOrBlank()) {
            return productService.findByName(name)
        }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Query parameter categoryId or name is required.")
    }
}
