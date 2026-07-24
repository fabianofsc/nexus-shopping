package com.nexus.shopping.order.application.usecase

import com.nexus.shopping.order.application.exception.OrderNotFoundException
import com.nexus.shopping.order.application.exception.OrderStateConflictException
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderStateTransitionException

class CancelOrderUseCase(
    private val orderRepository: OrderRepositoryPort,
) {
    fun execute(id: Long): Order {
        val order = orderRepository.findById(id) ?: throw OrderNotFoundException("Order $id not found.")
        return cancel(order)
    }

    fun executeForCustomer(
        customerId: Long,
        id: Long,
    ): Order {
        val order = orderRepository.findById(id) ?: throw OrderNotFoundException("Order $id not found.")
        if (order.customerId != customerId) throw OrderNotFoundException("Order $id not found.")
        return cancel(order)
    }

    private fun cancel(order: Order): Order {
        val cancelled =
            try {
                order.cancel()
            } catch (exception: OrderStateTransitionException) {
                throw OrderStateConflictException(exception.message ?: "Order cannot be cancelled.")
            }
        return orderRepository.update(cancelled)
    }
}
