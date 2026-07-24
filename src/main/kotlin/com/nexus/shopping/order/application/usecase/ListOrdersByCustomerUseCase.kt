package com.nexus.shopping.order.application.usecase

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
    ): PageResult<Order> = orderRepository.findByCustomerId(customerId, page, size)
}
