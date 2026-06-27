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
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): ProductPage {
        if (categoryId != null && !name.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Use either categoryId or name, not both.")
        }

        if (page < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Query parameter page must be greater than or equal to 0.")
        }

        if (size !in 1..500) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Query parameter size must be between 1 and 500.")
        }

        if (categoryId != null) {
            return productService.findByCategoryId(categoryId, page, size)
        }

        if (!name.isNullOrBlank()) {
            return productService.findByName(name, page, size)
        }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Query parameter categoryId or name is required.")
    }
}
