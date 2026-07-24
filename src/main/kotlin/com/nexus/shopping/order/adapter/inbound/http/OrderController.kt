package com.nexus.shopping.order.adapter.inbound.http

import com.nexus.shopping.order.adapter.inbound.http.dto.OrderResponse
import com.nexus.shopping.order.adapter.inbound.http.dto.toResponse
import com.nexus.shopping.order.application.usecase.CancelOrderUseCase
import com.nexus.shopping.order.application.usecase.GetOrderByIdUseCase
import com.nexus.shopping.order.application.usecase.ListOrdersByCustomerUseCase
import com.nexus.shopping.platform.adapter.inbound.http.dto.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/customers/{customerId}/orders")
class OrderController(
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
    private val listOrdersByCustomerUseCase: ListOrdersByCustomerUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
) {
    @GetMapping
    fun list(
        @PathVariable customerId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): PageResponse<OrderResponse> = listOrdersByCustomerUseCase.list(customerId, page, size).toResponse()

    @GetMapping("/{orderId}")
    fun getById(
        @PathVariable customerId: Long,
        @PathVariable orderId: Long,
    ): OrderResponse = getOrderByIdUseCase.executeForCustomer(customerId, orderId).toResponse()

    @PostMapping("/{orderId}/cancel")
    fun cancel(
        @PathVariable customerId: Long,
        @PathVariable orderId: Long,
    ): OrderResponse = cancelOrderUseCase.executeForCustomer(customerId, orderId).toResponse()
}
