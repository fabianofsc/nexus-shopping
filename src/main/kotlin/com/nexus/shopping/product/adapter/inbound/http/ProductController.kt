package com.nexus.shopping.product.adapter.inbound.http

import com.nexus.shopping.product.adapter.inbound.http.dto.CreateProductRequest
import com.nexus.shopping.product.adapter.inbound.http.dto.ProductPageResponse
import com.nexus.shopping.product.adapter.inbound.http.dto.ProductResponse
import com.nexus.shopping.product.adapter.inbound.http.dto.UpdatePriceRequest
import com.nexus.shopping.product.adapter.inbound.http.dto.toCommand
import com.nexus.shopping.product.adapter.inbound.http.dto.toResponse
import com.nexus.shopping.product.application.usecase.ProductCreateUseCase
import com.nexus.shopping.product.application.usecase.ProductSearchUseCase
import com.nexus.shopping.product.application.usecase.UpdateProductPriceUseCase
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
    ): ProductPageResponse = productSearchUseCase.search(categoryId, name, page, size).toResponse()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateProductRequest,
    ): ProductResponse = productCreateUseCase.create(request.toCommand()).toResponse()

    @PatchMapping("/{id}")
    fun updatePrice(
        @PathVariable id: Long,
        @RequestBody request: UpdatePriceRequest,
    ): ProductResponse = updateProductPriceUseCase.execute(request.toCommand(id)).toResponse()
}
