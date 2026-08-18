package com.nexus.shopping.product.adapter.inbound.http

import com.nexus.shopping.product.adapter.inbound.http.dto.CategoryResponse
import com.nexus.shopping.product.adapter.inbound.http.dto.CreateCategoryRequest
import com.nexus.shopping.product.adapter.inbound.http.dto.UpdateCategoryStatusRequest
import com.nexus.shopping.product.adapter.inbound.http.dto.toCommand
import com.nexus.shopping.product.adapter.inbound.http.dto.toResponse
import com.nexus.shopping.product.application.usecase.CreateCategoryUseCase
import com.nexus.shopping.product.application.usecase.ListCategoriesUseCase
import com.nexus.shopping.product.application.usecase.UpdateCategoryStatusUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/categories")
class CategoryController(
    private val listCategoriesUseCase: ListCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryStatusUseCase: UpdateCategoryStatusUseCase,
) {
    @GetMapping
    fun list(): List<CategoryResponse> = listCategoriesUseCase.list().map { it.toResponse() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateCategoryRequest,
    ): CategoryResponse = createCategoryUseCase.create(request.toCommand()).toResponse()

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody request: UpdateCategoryStatusRequest,
    ): CategoryResponse = updateCategoryStatusUseCase.execute(request.toCommand(id)).toResponse()
}
