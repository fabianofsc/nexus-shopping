package com.nexus.shopping.order.application.usecase

import com.nexus.shopping.order.application.command.CheckoutOrderCommand
import com.nexus.shopping.order.application.exception.OrderIdempotencyConflictException
import com.nexus.shopping.order.application.exception.OrderValidationException
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.OrderStatus
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
import java.security.MessageDigest

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
                items = command.items.toList(),
                status = OrderStatus.WAITING_PAYMENT,
                idempotencyKey = command.idempotencyKey,
                requestFingerprint = CheckoutRequestFingerprint.from(command),
                createdAt = null,
                cancelledAt = null,
            )
        val persistedOrder = orderRepository.createIfAbsentByCustomerIdAndIdempotencyKey(requestedOrder)

        if (persistedOrder.requestFingerprint != requestedOrder.requestFingerprint) {
            throw OrderIdempotencyConflictException(
                "Idempotency key ${command.idempotencyKey} was already used with a different payload.",
            )
        }
        return persistedOrder
    }

    private fun invalid(message: String): Nothing = throw OrderValidationException(message)
}

private object CheckoutRequestFingerprint {
    fun from(command: CheckoutOrderCommand): String {
        val canonicalPayload =
            buildString {
                appendField(command.customerId.toString())
                appendField(command.cartId.toString())
                appendCustomer(command.customerSnapshot)
                appendShippingAddress(command.shippingAddressSnapshot)
                command.items
                    .map(::canonicalItem)
                    .sorted()
                    .forEach { item -> appendField(item) }
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

    private fun canonicalItem(item: OrderItemSnapshot): String =
        buildString {
            appendField(item.productId.toString())
            appendField(item.productName)
            appendField(item.unitPriceAmount.stripTrailingZeros().toPlainString())
            appendField(item.currency.name)
            appendField(item.quantity.toString())
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
