package com.nexus.shopping.order.application.usecase

import com.nexus.shopping.order.application.exception.OrderNotFoundException
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.domain.Order

class GetOrderByIdUseCase(
    private val orderRepository: OrderRepositoryPort,
) {
    fun execute(id: Long): Order = orderRepository.findById(id) ?: throw OrderNotFoundException("Order $id not found.")
}
