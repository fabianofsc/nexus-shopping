package com.nexus.shopping.order.application.usecase

import com.nexus.shopping.order.application.command.ApplyOrderPaymentResultCommand
import com.nexus.shopping.order.application.exception.OrderNotFoundException
import com.nexus.shopping.order.application.exception.OrderStateConflictException
import com.nexus.shopping.order.application.exception.OrderValidationException
import com.nexus.shopping.order.application.port.inbound.ApplyOrderPaymentResultInputPort
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.port.outbound.TransactionPort
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderPaymentResultStatus
import com.nexus.shopping.order.domain.OrderStateTransitionException
import org.springframework.stereotype.Service

@Service
class ApplyOrderPaymentResultUseCase(
    private val orderRepository: OrderRepositoryPort,
    private val transaction: TransactionPort = ImmediatePaymentResultTransaction,
) : ApplyOrderPaymentResultInputPort {
    override fun apply(command: ApplyOrderPaymentResultCommand): Order =
        transaction.inTransaction {
            validate(command)
            val order =
                orderRepository.findByIdForUpdate(command.orderId)
                    ?: throw OrderNotFoundException("Order ${command.orderId} not found.")
            if (order.paymentAttemptReference == command.attemptReference) return@inTransaction order

            val updated =
                try {
                    order.applyPaymentResult(
                        attemptReference = command.attemptReference,
                        result = OrderPaymentResultStatus.valueOf(command.status),
                        providerTransactionId = command.providerTransactionId,
                    )
                } catch (exception: OrderStateTransitionException) {
                    throw OrderStateConflictException(exception.message ?: "Payment result cannot be applied to order.")
                }
            orderRepository.update(updated)
        }

    private fun validate(command: ApplyOrderPaymentResultCommand) {
        if (command.orderId <= 0) throw OrderValidationException("orderId must be greater than 0.")
        if (command.attemptReference.isBlank()) throw OrderValidationException("attemptReference must not be blank.")
        if (command.attemptReference.length > 255) {
            throw OrderValidationException("attemptReference must be at most 255 characters.")
        }
        if (command.status !in OrderPaymentResultStatus.entries.map { it.name }) {
            throw OrderValidationException("status must be APPROVED or REJECTED.")
        }
        if (command.providerTransactionId != null && command.providerTransactionId.length > 255) {
            throw OrderValidationException("providerTransactionId must be at most 255 characters.")
        }
    }
}

private object ImmediatePaymentResultTransaction : TransactionPort {
    override fun <T> inTransaction(block: () -> T): T = block()
}
