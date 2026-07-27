package com.nexus.shopping.order.application.usecase

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.inbound.CartCheckoutInputPort
import com.nexus.shopping.order.application.command.CheckoutOrderCommand
import com.nexus.shopping.order.application.exception.OrderIdempotencyConflictException
import com.nexus.shopping.order.application.exception.OrderValidationException
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.port.outbound.TransactionPort
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.OrderStatus
import org.springframework.stereotype.Service

@Service
class CheckoutOrderUseCase(
    private val orderRepository: OrderRepositoryPort,
    private val cartCheckout: CartCheckoutInputPort,
    private val transaction: TransactionPort,
) {
    fun execute(command: CheckoutOrderCommand): Order = executeWithResult(command).order

    fun executeWithResult(command: CheckoutOrderCommand): CheckoutOrderResult {
        OrderCreationPayloadValidator.validateBase(
            customerId = command.customerId,
            customerSnapshot = command.customerSnapshot,
            shippingAddressSnapshot = command.shippingAddressSnapshot,
            idempotencyKey = command.idempotencyKey,
        )
        val fingerprint =
            OrderCreationRequestFingerprint.from(
                customerId = command.customerId,
                customerSnapshot = command.customerSnapshot,
                shippingAddressSnapshot = command.shippingAddressSnapshot,
            )

        return transaction.inTransaction {
            findReplay(command, fingerprint)?.let { return@inTransaction CheckoutOrderResult(it, replayed = true) }

            val lockedCart =
                try {
                    cartCheckout.reserveActiveCart(command.customerId)
                } catch (exception: CartValidationException) {
                    findReplay(command, fingerprint)?.let { return@inTransaction CheckoutOrderResult(it, replayed = true) }
                    throw exception
                }
            findReplay(command, fingerprint)?.let { return@inTransaction CheckoutOrderResult(it, replayed = true) }
            if (lockedCart.items.isEmpty()) invalid("cart items must not be empty.")
            val itemSnapshots = lockedCart.items.map { it.toOrderItemSnapshot() }
            OrderCreationPayloadValidator.validateItems(itemSnapshots)

            val requestedOrder =
                Order(
                    id = null,
                    customerId = command.customerId,
                    cartId = lockedCart.id,
                    customerSnapshot = command.customerSnapshot,
                    shippingAddressSnapshot = command.shippingAddressSnapshot,
                    items = itemSnapshots,
                    status = OrderStatus.WAITING_PAYMENT,
                    idempotencyKey = command.idempotencyKey,
                    requestFingerprint = fingerprint,
                    createdAt = null,
                    cancelledAt = null,
                )
            val persistedOrder = orderRepository.create(requestedOrder)

            if (persistedOrder.requestFingerprint != fingerprint) {
                conflict(command.idempotencyKey)
            }
            if (persistedOrder.cartId == lockedCart.id) {
                cartCheckout.confirmCheckout(lockedCart.id)
            }
            CheckoutOrderResult(persistedOrder, replayed = false)
        }
    }

    private fun findReplay(
        command: CheckoutOrderCommand,
        fingerprint: String,
    ): Order? =
        orderRepository.findByCustomerIdAndIdempotencyKey(command.customerId, command.idempotencyKey)?.also {
            if (it.requestFingerprint != fingerprint) conflict(command.idempotencyKey)
        }

    private fun conflict(idempotencyKey: String): Nothing =
        throw OrderIdempotencyConflictException("Idempotency key $idempotencyKey was already used with a different payload.")

    private fun invalid(message: String): Nothing = throw OrderValidationException(message)
}

private fun com.nexus.shopping.cart.domain.CartItem.toOrderItemSnapshot() =
    OrderItemSnapshot(
        productId = productSummary.productId,
        productName = productSummary.name,
        unitPriceAmount = productSummary.unitPriceAmount,
        currency =
            com.nexus.shopping.order.domain.Currency
                .valueOf(productSummary.currency.name),
        quantity = quantity,
    )

data class CheckoutOrderResult(
    val order: Order,
    val replayed: Boolean,
)
