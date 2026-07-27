package com.nexus.shopping.integration.checkout.adapter.inbound.http

import com.nexus.shopping.integration.checkout.adapter.inbound.http.dto.CheckoutRequest
import com.nexus.shopping.integration.checkout.adapter.inbound.http.dto.CheckoutResponse
import com.nexus.shopping.integration.checkout.adapter.inbound.http.dto.toCommand
import com.nexus.shopping.integration.checkout.adapter.inbound.http.dto.toResponse
import com.nexus.shopping.integration.checkout.application.CheckoutWorkflowUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/customers/{customerId}/cart")
class CheckoutController(
    private val checkout: CheckoutWorkflowUseCase,
) {
    @PostMapping("/checkout")
    fun checkout(
        @PathVariable customerId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: CheckoutRequest,
    ): ResponseEntity<CheckoutResponse> {
        val result = checkout.execute(request.toCommand(customerId, idempotencyKey))
        val status = if (result.replayed) HttpStatus.OK else HttpStatus.CREATED
        return ResponseEntity.status(status).body(result.toResponse())
    }
}
