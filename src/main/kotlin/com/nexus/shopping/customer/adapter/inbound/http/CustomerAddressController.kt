package com.nexus.shopping.customer.adapter.inbound.http

import com.nexus.shopping.customer.adapter.inbound.http.dto.AddressResponse
import com.nexus.shopping.customer.adapter.inbound.http.dto.UpdateCustomerAddressRequest
import com.nexus.shopping.customer.adapter.inbound.http.dto.toCommand
import com.nexus.shopping.customer.adapter.inbound.http.dto.toResponse
import com.nexus.shopping.customer.application.usecase.GetCustomerByIdUseCase
import com.nexus.shopping.customer.application.usecase.UpdateCustomerAddressUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/customers/{customerId}/address")
class CustomerAddressController(
    private val getCustomerByIdUseCase: GetCustomerByIdUseCase,
    private val updateCustomerAddressUseCase: UpdateCustomerAddressUseCase,
) {
    @GetMapping
    fun get(
        @PathVariable customerId: Long,
    ): AddressResponse = getCustomerByIdUseCase.execute(customerId).address.toResponse()

    @PutMapping
    fun update(
        @PathVariable customerId: Long,
        @RequestBody request: UpdateCustomerAddressRequest,
    ): AddressResponse = updateCustomerAddressUseCase.execute(customerId, request.toCommand()).address.toResponse()
}
