package com.nexus.shopping.order.application.usecase

import com.nexus.shopping.order.application.exception.OrderNotFoundException
import com.nexus.shopping.order.application.exception.OrderStateConflictException
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.port.outbound.ReleaseStockPort
import com.nexus.shopping.order.application.port.outbound.ReleasedStockItem
import com.nexus.shopping.order.application.port.outbound.TransactionPort
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderStateTransitionException
import org.springframework.stereotype.Service

@Service
class CancelOrderUseCase(
    private val orderRepository: OrderRepositoryPort,
    private val transaction: TransactionPort = ImmediateTransaction,
    private val releaseStock: ReleaseStockPort = NoopReleaseStock,
) {
    fun execute(id: Long): Order = transaction.inTransaction { cancel(id, null) }

    fun executeForCustomer(
        customerId: Long,
        id: Long,
    ): Order = transaction.inTransaction { cancel(id, customerId) }

    private fun cancel(
        id: Long,
        customerId: Long?,
    ): Order {
        val order = orderRepository.findByIdForUpdate(id) ?: throw OrderNotFoundException("Order $id not found.")
        if (customerId != null && order.customerId != customerId) throw OrderNotFoundException("Order $id not found.")
        val cancelled =
            try {
                order.cancel()
            } catch (exception: OrderStateTransitionException) {
                throw OrderStateConflictException(exception.message ?: "Order cannot be cancelled.")
            }
        orderRepository.update(cancelled)
        releaseStock.release(
            orderReference = "checkout:${requireNotNull(order.id)}",
            items = order.items.map { ReleasedStockItem(it.productId, it.quantity) },
        )
        return cancelled
    }
}

private object NoopReleaseStock : ReleaseStockPort {
    override fun release(
        orderReference: String,
        items: List<ReleasedStockItem>,
    ) = Unit
}

private object ImmediateTransaction : TransactionPort {
    override fun <T> inTransaction(block: () -> T): T = block()
}
