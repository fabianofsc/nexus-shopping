package com.nexus.shopping.order.application.usecase

import com.nexus.shopping.order.application.command.CheckoutOrderCommand
import com.nexus.shopping.order.application.exception.OrderIdempotencyConflictException
import com.nexus.shopping.order.application.exception.OrderValidationException
import com.nexus.shopping.order.application.port.outbound.CartCheckoutPort
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.port.outbound.TransactionPort
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderStatus
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
import java.security.MessageDigest

class CheckoutOrderUseCase(
    private val orderRepository: OrderRepositoryPort,
    private val cartCheckout: CartCheckoutPort,
    private val transaction: TransactionPort,
) {
    fun execute(command: CheckoutOrderCommand): Order {
        if (command.idempotencyKey.isBlank()) invalid("idempotencyKey must not be blank.")
        if (command.customerSnapshot.customerId != command.customerId) {
            invalid("customerSnapshot.customerId must match customerId.")
        }
        val fingerprint = CheckoutRequestFingerprint.from(command)

        return transaction.inTransaction {
            findReplay(command, fingerprint)?.let { return@inTransaction it }

            val lockedCart = cartCheckout.lockActiveCartByCustomerId(command.customerId)
            if (lockedCart == null) {
                findReplay(command, fingerprint)?.let { return@inTransaction it }
                invalid("customerId ${command.customerId} does not have an active cart.")
            }
            findReplay(command, fingerprint)?.let { return@inTransaction it }
            if (lockedCart.items.isEmpty()) invalid("cart items must not be empty.")

            val requestedOrder =
                Order(
                    id = null,
                    customerId = command.customerId,
                    cartId = lockedCart.cartId,
                    customerSnapshot = command.customerSnapshot,
                    shippingAddressSnapshot = command.shippingAddressSnapshot,
                    items = lockedCart.items,
                    status = OrderStatus.WAITING_PAYMENT,
                    idempotencyKey = command.idempotencyKey,
                    requestFingerprint = fingerprint,
                    createdAt = null,
                    cancelledAt = null,
                )
            val persistedOrder = orderRepository.createIfAbsentByCustomerIdAndIdempotencyKey(requestedOrder)

            if (persistedOrder.requestFingerprint != fingerprint) {
                conflict(command.idempotencyKey)
            }
            if (persistedOrder.cartId == lockedCart.cartId) {
                cartCheckout.markCheckedOut(lockedCart.cartId)
            }
            persistedOrder
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

private object CheckoutRequestFingerprint {
    fun from(command: CheckoutOrderCommand): String {
        val canonicalPayload =
            buildString {
                appendField(command.customerId.toString())
                appendCustomer(command.customerSnapshot)
                appendShippingAddress(command.shippingAddressSnapshot)
            }
        return MessageDigest
            .getInstance("SHA-256")
            .digest(canonicalPayload.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun StringBuilder.appendCustomer(snapshot: CustomerSnapshot) {
        appendField(snapshot.customerId.toString())
        appendField(snapshot.name)
        appendField(snapshot.document)
        appendField(snapshot.documentType)
        appendField(snapshot.email)
        appendNullable(snapshot.phone)
    }

    private fun StringBuilder.appendShippingAddress(snapshot: ShippingAddressSnapshot) {
        appendField(snapshot.street)
        appendField(snapshot.number)
        appendNullable(snapshot.complement)
        appendField(snapshot.neighborhood)
        appendField(snapshot.city)
        appendField(snapshot.state)
        appendField(snapshot.zipCode)
        appendField(snapshot.country)
    }

    private fun StringBuilder.appendNullable(value: String?) {
        appendField(value)
    }

    private fun StringBuilder.appendField(value: String?) {
        if (value == null) {
            append("-1:")
        } else {
            append(value.length).append(':').append(value)
        }
    }
}
