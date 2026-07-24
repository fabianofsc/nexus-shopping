package com.nexus.shopping.order.application.usecase

import com.nexus.shopping.order.application.command.CheckoutOrderCommand
import com.nexus.shopping.order.application.exception.OrderIdempotencyConflictException
import com.nexus.shopping.order.application.exception.OrderValidationException
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderStatus

class CheckoutOrderUseCase(
    private val orderRepository: OrderRepositoryPort,
) {
    fun execute(command: CheckoutOrderCommand): Order {
        if (command.idempotencyKey.isBlank()) invalid("idempotencyKey must not be blank.")
        if (command.items.isEmpty()) invalid("items must not be empty.")
        if (command.customerSnapshot.customerId != command.customerId) {
            invalid("customerSnapshot.customerId must match customerId.")
        }

        val requestedOrder =
            Order(
                id = null,
                customerId = command.customerId,
                cartId = command.cartId,
                customerSnapshot = command.customerSnapshot,
                shippingAddressSnapshot = command.shippingAddressSnapshot,
                items = command.items,
                status = OrderStatus.WAITING_PAYMENT,
                idempotencyKey = command.idempotencyKey,
                requestFingerprint = command.requestFingerprint,
                createdAt = null,
                cancelledAt = null,
            )
        val persistedOrder = orderRepository.createIfAbsentByCustomerIdAndIdempotencyKey(requestedOrder)

        if (persistedOrder.requestFingerprint != command.requestFingerprint) {
            throw OrderIdempotencyConflictException(
                "Idempotency key ${command.idempotencyKey} was already used with a different payload.",
            )
        }
        return persistedOrder
    }

    private fun invalid(message: String): Nothing = throw OrderValidationException(message)
}
