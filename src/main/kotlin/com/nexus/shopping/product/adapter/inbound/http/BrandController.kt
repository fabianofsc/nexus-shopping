package com.nexus.shopping.product.adapter.inbound.http

import com.nexus.shopping.product.adapter.inbound.http.dto.BrandResponse
import com.nexus.shopping.product.adapter.inbound.http.dto.CreateBrandRequest
import com.nexus.shopping.product.adapter.inbound.http.dto.toCommand
import com.nexus.shopping.product.adapter.inbound.http.dto.toResponse
import com.nexus.shopping.product.application.usecase.CreateBrandUseCase
import com.nexus.shopping.product.application.usecase.ListBrandsUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/brands")
class BrandController(
    private val listBrandsUseCase: ListBrandsUseCase,
    private val createBrandUseCase: CreateBrandUseCase,
) {
    @GetMapping
    fun list(): List<BrandResponse> = listBrandsUseCase.list().map { it.toResponse() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateBrandRequest,
    ): BrandResponse = createBrandUseCase.create(request.toCommand()).toResponse()
}
