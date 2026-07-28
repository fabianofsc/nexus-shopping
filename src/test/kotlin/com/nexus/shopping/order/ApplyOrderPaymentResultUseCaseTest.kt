package com.nexus.shopping.order

import com.nexus.shopping.order.application.command.ApplyOrderPaymentResultCommand
import com.nexus.shopping.order.application.exception.OrderStateConflictException
import com.nexus.shopping.order.application.port.outbound.OrderPersistenceResult
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.usecase.ApplyOrderPaymentResultUseCase
import com.nexus.shopping.order.domain.Currency
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.OrderStatus
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
import com.nexus.shopping.platform.domain.PageResult
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class PaymentResultOrderRepository(
    initial: Order,
) : OrderRepositoryPort {
    var current = initial
    var updates = 0

    override fun findById(id: Long): Order? = current.takeIf { it.id == id }

    override fun findByIdForUpdate(id: Long): Order? = findById(id)

    override fun update(order: Order): Order {
        updates += 1
        current = order
        return order
    }

    override fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): Order? = null

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Order> = throw UnsupportedOperationException()

    override fun create(order: Order): OrderPersistenceResult = throw UnsupportedOperationException()
}

class ApplyOrderPaymentResultUseCaseTest {
    @Test
    fun `applies an external approved result without importing Payment types`() {
        val repository = PaymentResultOrderRepository(waitingOrder())
        val useCase = ApplyOrderPaymentResultUseCase(repository)

        val result =
            useCase.apply(
                ApplyOrderPaymentResultCommand(
                    orderId = 1L,
                    attemptReference = "pay_attempt_1",
                    status = "APPROVED",
                    providerTransactionId = "provider_tx_1",
                ),
            )

        assertEquals(OrderStatus.CONFIRMED, result.status)
        assertEquals(1, repository.updates)
    }

    @Test
    fun `does not persist again when the same attempt result is replayed`() {
        val repository = PaymentResultOrderRepository(waitingOrder())
        val useCase = ApplyOrderPaymentResultUseCase(repository)
        val command =
            ApplyOrderPaymentResultCommand(
                orderId = 1L,
                attemptReference = "pay_attempt_1",
                status = "APPROVED",
                providerTransactionId = "provider_tx_1",
            )

        val first = useCase.apply(command)
        val replay = useCase.apply(command)

        assertEquals(first, replay)
        assertEquals(1, repository.updates)
    }

    @Test
    fun `rejects a conflicting result from a different attempt`() {
        val repository = PaymentResultOrderRepository(waitingOrder())
        val useCase = ApplyOrderPaymentResultUseCase(repository)
        useCase.apply(
            ApplyOrderPaymentResultCommand(1L, "pay_attempt_1", "APPROVED", "provider_tx_1"),
        )

        assertFailsWith<OrderStateConflictException> {
            useCase.apply(
                ApplyOrderPaymentResultCommand(1L, "pay_attempt_2", "REJECTED", null),
            )
        }
    }

    private fun waitingOrder() =
        Order(
            id = 1L,
            customerId = 10L,
            cartId = 100L,
            customerSnapshot = CustomerSnapshot(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null),
            shippingAddressSnapshot =
                ShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
            items = listOf(OrderItemSnapshot(1L, "Produto A", BigDecimal("19.90"), Currency.BRL, 2)),
            status = OrderStatus.WAITING_PAYMENT,
            idempotencyKey = "checkout-1",
            requestFingerprint = "fingerprint",
            createdAt = null,
            cancelledAt = null,
        )
}
