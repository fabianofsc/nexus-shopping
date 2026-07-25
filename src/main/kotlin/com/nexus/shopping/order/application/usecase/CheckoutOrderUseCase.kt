package com.nexus.shopping.order.application.usecase

import com.nexus.shopping.order.application.command.CheckoutOrderCommand
import com.nexus.shopping.order.application.exception.OrderIdempotencyConflictException
import com.nexus.shopping.order.application.exception.OrderValidationException
import com.nexus.shopping.order.application.port.outbound.CartCheckoutPort
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.port.outbound.TransactionPort
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.OrderStatus
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class CheckoutOrderUseCase(
    private val orderRepository: OrderRepositoryPort,
    private val cartCheckout: CartCheckoutPort,
    private val transaction: TransactionPort,
) {
    fun execute(command: CheckoutOrderCommand): Order = executeWithResult(command).order

    fun executeWithResult(command: CheckoutOrderCommand): CheckoutOrderResult {
        CheckoutPayloadValidator.validate(command)
        val fingerprint = CheckoutRequestFingerprint.from(command)

        return transaction.inTransaction {
            findReplay(command, fingerprint)?.let { return@inTransaction CheckoutOrderResult(it, replayed = true) }

            val lockedCart = cartCheckout.lockActiveCartByCustomerId(command.customerId)
            if (lockedCart == null) {
                findReplay(command, fingerprint)?.let { return@inTransaction CheckoutOrderResult(it, replayed = true) }
                invalid("customerId ${command.customerId} does not have an active cart.")
            }
            findReplay(command, fingerprint)?.let { return@inTransaction CheckoutOrderResult(it, replayed = true) }
            if (lockedCart.items.isEmpty()) invalid("cart items must not be empty.")
            CheckoutPayloadValidator.validateItems(lockedCart.items)

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
            val persistedOrder = orderRepository.create(requestedOrder)

            if (persistedOrder.requestFingerprint != fingerprint) {
                conflict(command.idempotencyKey)
            }
            if (persistedOrder.cartId == lockedCart.cartId) {
                cartCheckout.markCheckedOut(lockedCart.cartId)
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

data class CheckoutOrderResult(
    val order: Order,
    val replayed: Boolean,
)

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

private object CheckoutPayloadValidator {
    fun validate(command: CheckoutOrderCommand) {
        positive(command.customerId, "customerId")
        length(command.idempotencyKey, "idempotencyKey", 255)
        required(command.idempotencyKey, "idempotencyKey")
        if (command.customerSnapshot.customerId != command.customerId) {
            invalid("customerSnapshot.customerId must match customerId.")
        }
        validateCustomer(command.customerSnapshot)
        validateShippingAddress(command.shippingAddressSnapshot)
    }

    fun validateItems(items: List<OrderItemSnapshot>) {
        if (items.map { it.currency }.toSet().size != 1) {
            invalid("cart items must use a single currency.")
        }
        items.forEachIndexed { index, item ->
            positive(item.productId, "items[$index].productId")
            required(item.productName, "items[$index].productName")
            length(item.productName, "items[$index].productName", 220)
            if (item.unitPriceAmount.signum() < 0) invalid("items[$index].unitPriceAmount must not be negative.")
            val normalizedAmount = item.unitPriceAmount.stripTrailingZeros()
            val effectiveScale = normalizedAmount.scale().coerceAtLeast(0)
            val integerDigits = (normalizedAmount.precision() - normalizedAmount.scale()).coerceAtLeast(1)
            if (effectiveScale > 2 || integerDigits > 10) {
                invalid("items[$index].unitPriceAmount exceeds NUMERIC(12, 2).")
            }
            if (item.quantity <= 0) invalid("items[$index].quantity must be greater than zero.")
        }
    }

    private fun validateCustomer(snapshot: CustomerSnapshot) {
        required(snapshot.name, "customerSnapshot.name")
        length(snapshot.name, "customerSnapshot.name", 220)
        required(snapshot.document, "customerSnapshot.document")
        length(snapshot.document, "customerSnapshot.document", 64)
        required(snapshot.documentType, "customerSnapshot.documentType")
        length(snapshot.documentType, "customerSnapshot.documentType", 32)
        required(snapshot.email, "customerSnapshot.email")
        length(snapshot.email, "customerSnapshot.email", 320)
        nullableLength(snapshot.phone, "customerSnapshot.phone", 64)
    }

    private fun validateShippingAddress(snapshot: ShippingAddressSnapshot) {
        required(snapshot.street, "shippingAddressSnapshot.street")
        length(snapshot.street, "shippingAddressSnapshot.street", 220)
        required(snapshot.number, "shippingAddressSnapshot.number")
        length(snapshot.number, "shippingAddressSnapshot.number", 32)
        nullableLength(snapshot.complement, "shippingAddressSnapshot.complement", 220)
        required(snapshot.neighborhood, "shippingAddressSnapshot.neighborhood")
        length(snapshot.neighborhood, "shippingAddressSnapshot.neighborhood", 220)
        required(snapshot.city, "shippingAddressSnapshot.city")
        length(snapshot.city, "shippingAddressSnapshot.city", 160)
        required(snapshot.state, "shippingAddressSnapshot.state")
        length(snapshot.state, "shippingAddressSnapshot.state", 80)
        required(snapshot.zipCode, "shippingAddressSnapshot.zipCode")
        length(snapshot.zipCode, "shippingAddressSnapshot.zipCode", 32)
        required(snapshot.country, "shippingAddressSnapshot.country")
        length(snapshot.country, "shippingAddressSnapshot.country", 80)
    }

    private fun positive(
        value: Long,
        field: String,
    ) {
        if (value <= 0) invalid("$field must be greater than zero.")
    }

    private fun required(
        value: String,
        field: String,
    ) {
        if (value.isBlank()) invalid("$field must not be blank.")
    }

    private fun length(
        value: String,
        field: String,
        maximum: Int,
    ) {
        if (value.length > maximum) invalid("$field must not exceed $maximum characters.")
    }

    private fun nullableLength(
        value: String?,
        field: String,
        maximum: Int,
    ) {
        if (value != null) length(value, field, maximum)
    }

    private fun invalid(message: String): Nothing = throw OrderValidationException(message)
}
