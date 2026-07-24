package com.nexus.shopping.order.application.usecase

import com.nexus.shopping.order.application.exception.OrderValidationException
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.platform.domain.PageResult

class ListOrdersByCustomerUseCase(
    private val orderRepository: OrderRepositoryPort,
) {
    fun list(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Order> {
        if (page < 0) throw OrderValidationException("page must be greater than or equal to 0.")
        if (size !in 1..500) throw OrderValidationException("size must be between 1 and 500.")
        return orderRepository.findByCustomerId(customerId, page, size)
    }
}
