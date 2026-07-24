package com.nexus.shopping.order.adapter.inbound.http

import com.nexus.shopping.order.adapter.inbound.http.dto.CheckoutOrderRequest
import com.nexus.shopping.order.adapter.inbound.http.dto.OrderResponse
import com.nexus.shopping.order.adapter.inbound.http.dto.toCommand
import com.nexus.shopping.order.adapter.inbound.http.dto.toResponse
import com.nexus.shopping.order.application.usecase.CheckoutOrderUseCase
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
class OrderCheckoutController(
    private val checkoutOrderUseCase: CheckoutOrderUseCase,
) {
    @PostMapping("/checkout")
    fun checkout(
        @PathVariable customerId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: CheckoutOrderRequest,
    ): ResponseEntity<OrderResponse> {
        val result = checkoutOrderUseCase.executeWithResult(request.toCommand(customerId, idempotencyKey))
        val status = if (result.created) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(result.order.toResponse())
    }
}
