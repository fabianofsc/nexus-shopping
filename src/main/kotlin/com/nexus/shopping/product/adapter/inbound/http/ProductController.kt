package com.nexus.shopping.product.adapter.inbound.http

import com.nexus.shopping.product.application.usecase.ProductCreateUseCase
import com.nexus.shopping.product.application.usecase.ProductSearchUseCase
import com.nexus.shopping.product.application.usecase.UpdateProductPriceUseCase
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/products")
class ProductController(
    private val productSearchUseCase: ProductSearchUseCase,
    private val productCreateUseCase: ProductCreateUseCase,
    private val updateProductPriceUseCase: UpdateProductPriceUseCase,
) {

    @GetMapping
    fun search(
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) name: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): ProductPage =
        productSearchUseCase.search(categoryId, name, page, size)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateProductRequest): Product =
        productCreateUseCase.create(request.toCommand())

    @PatchMapping("/{id}")
    fun updatePrice(
        @PathVariable id: Long,
        @RequestBody request: UpdatePriceRequest,
    ): Product =
        updateProductPriceUseCase.execute(request.toCommand(id))
}
