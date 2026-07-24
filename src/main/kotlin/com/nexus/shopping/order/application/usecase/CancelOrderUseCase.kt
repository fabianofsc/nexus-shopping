package com.nexus.shopping.order.application.usecase

import com.nexus.shopping.order.application.exception.OrderNotFoundException
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.domain.Order

class CancelOrderUseCase(
    private val orderRepository: OrderRepositoryPort,
) {
    fun execute(id: Long): Order {
        val order = orderRepository.findById(id) ?: throw OrderNotFoundException("Order $id not found.")
        return orderRepository.update(order.cancel())
    }
}
