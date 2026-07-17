package com.nexus.shopping.customer.adapter.inbound.http

import com.nexus.shopping.customer.adapter.inbound.http.dto.CreateCustomerRequest
import com.nexus.shopping.customer.adapter.inbound.http.dto.CustomerResponse
import com.nexus.shopping.customer.adapter.inbound.http.dto.toCommand
import com.nexus.shopping.customer.adapter.inbound.http.dto.toResponse
import com.nexus.shopping.customer.application.usecase.CreateCustomerUseCase
import com.nexus.shopping.customer.application.usecase.GetCustomerByIdUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/customers")
class CustomerController(
    private val createCustomerUseCase: CreateCustomerUseCase,
    private val getCustomerByIdUseCase: GetCustomerByIdUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateCustomerRequest,
    ): CustomerResponse = createCustomerUseCase.create(request.toCommand()).toResponse()

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
    ): CustomerResponse = getCustomerByIdUseCase.execute(id).toResponse()
}
