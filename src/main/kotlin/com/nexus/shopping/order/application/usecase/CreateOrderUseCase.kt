package com.nexus.shopping.order.application.usecase

import com.nexus.shopping.order.application.command.CreateOrderCommand
import com.nexus.shopping.order.application.exception.OrderIdempotencyConflictException
import com.nexus.shopping.order.application.exception.OrderValidationException
import com.nexus.shopping.order.application.port.inbound.CreateOrderInputPort
import com.nexus.shopping.order.application.port.inbound.CreatedOrder
import com.nexus.shopping.order.application.port.inbound.FindOrderReplayCommand
import com.nexus.shopping.order.application.port.inbound.FindOrderReplayInputPort
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.OrderStatus
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class CreateOrderUseCase(
    private val orderRepository: OrderRepositoryPort,
) : CreateOrderInputPort,
    FindOrderReplayInputPort {
    override fun findReplay(command: FindOrderReplayCommand): CreatedOrder? {
        OrderCreationPayloadValidator.validateBase(
            customerId = command.customerId,
            customerSnapshot = command.customerSnapshot,
            shippingAddressSnapshot = command.shippingAddressSnapshot,
            idempotencyKey = command.idempotencyKey,
        )
        val existing =
            orderRepository.findByCustomerIdAndIdempotencyKey(command.customerId, command.idempotencyKey)
                ?: return null
        val replayFingerprint =
            OrderCreationRequestFingerprint.current(
                customerId = command.customerId,
                cartId = existing.cartId,
                customerSnapshot = command.customerSnapshot,
                shippingAddressSnapshot = command.shippingAddressSnapshot,
                items = existing.items,
            )
        val legacyReplayFingerprint =
            OrderCreationRequestFingerprint.legacy(
                customerId = command.customerId,
                customerSnapshot = command.customerSnapshot,
                shippingAddressSnapshot = command.shippingAddressSnapshot,
            )
        if (existing.requestFingerprint != replayFingerprint && existing.requestFingerprint != legacyReplayFingerprint) {
            conflict(command.idempotencyKey)
        }
        return CreatedOrder(existing, replayed = true)
    }

    override fun create(command: CreateOrderCommand): CreatedOrder {
        OrderCreationPayloadValidator.validateBase(
            customerId = command.customerId,
            customerSnapshot = command.customerSnapshot,
            shippingAddressSnapshot = command.shippingAddressSnapshot,
            idempotencyKey = command.idempotencyKey,
        )
        OrderCreationPayloadValidator.validateCart(command.cartId, command.items)
        val fingerprint =
            OrderCreationRequestFingerprint.current(
                customerId = command.customerId,
                cartId = command.cartId,
                customerSnapshot = command.customerSnapshot,
                shippingAddressSnapshot = command.shippingAddressSnapshot,
                items = command.items,
            )
        val existing = orderRepository.findByCustomerIdAndIdempotencyKey(command.customerId, command.idempotencyKey)
        if (existing != null) {
            validateReplay(existing, command, fingerprint)
            return CreatedOrder(existing, replayed = true)
        }

        val order =
            Order(
                id = null,
                customerId = command.customerId,
                cartId = command.cartId,
                customerSnapshot = command.customerSnapshot,
                shippingAddressSnapshot = command.shippingAddressSnapshot,
                items = command.items,
                status = OrderStatus.WAITING_PAYMENT,
                idempotencyKey = command.idempotencyKey,
                requestFingerprint = fingerprint,
                createdAt = null,
                cancelledAt = null,
            )
        val persistenceResult = orderRepository.create(order)
        validateReplay(persistenceResult.order, command, fingerprint)
        return CreatedOrder(persistenceResult.order, replayed = !persistenceResult.created)
    }

    private fun validateReplay(
        existing: Order,
        command: CreateOrderCommand,
        currentFingerprint: String,
    ) {
        if (existing.requestFingerprint == currentFingerprint) return

        val legacyFingerprint =
            OrderCreationRequestFingerprint.legacy(
                customerId = command.customerId,
                customerSnapshot = command.customerSnapshot,
                shippingAddressSnapshot = command.shippingAddressSnapshot,
            )
        val sameCompletePayload =
            existing.requestFingerprint == legacyFingerprint &&
                existing.cartId == command.cartId &&
                existing.items == command.items
        if (!sameCompletePayload) conflict(command.idempotencyKey)
    }

    private fun conflict(idempotencyKey: String): Nothing =
        throw OrderIdempotencyConflictException("Idempotency key $idempotencyKey was already used with a different payload.")
}

internal object OrderCreationRequestFingerprint {
    /** Current V2 format: base snapshots plus cart id and every ordered item field. */
    fun current(
        customerId: Long,
        cartId: Long,
        customerSnapshot: CustomerSnapshot,
        shippingAddressSnapshot: ShippingAddressSnapshot,
        items: List<OrderItemSnapshot>,
    ): String {
        val canonicalPayload =
            buildString {
                appendField(customerId.toString())
                appendField(cartId.toString())
                appendCustomer(customerSnapshot)
                appendShippingAddress(shippingAddressSnapshot)
                appendItems(items)
            }
        return digest(canonicalPayload)
    }

    /**
     * Legacy V1 format persisted before checkout integration boundaries. The fallback is read-only:
     * new orders always store V2, while legacy replays additionally compare persisted cart/items.
     */
    fun legacy(
        customerId: Long,
        customerSnapshot: CustomerSnapshot,
        shippingAddressSnapshot: ShippingAddressSnapshot,
    ): String {
        val canonicalPayload =
            buildString {
                appendField(customerId.toString())
                appendCustomer(customerSnapshot)
                appendShippingAddress(shippingAddressSnapshot)
            }
        return digest(canonicalPayload)
    }

    private fun StringBuilder.appendCustomer(snapshot: CustomerSnapshot) {
        appendField(snapshot.customerId.toString())
        appendField(snapshot.name)
        appendField(snapshot.document)
        appendField(snapshot.documentType)
        appendField(snapshot.email)
        appendField(snapshot.phone)
    }

    private fun StringBuilder.appendShippingAddress(snapshot: ShippingAddressSnapshot) {
        appendField(snapshot.street)
        appendField(snapshot.number)
        appendField(snapshot.complement)
        appendField(snapshot.neighborhood)
        appendField(snapshot.city)
        appendField(snapshot.state)
        appendField(snapshot.zipCode)
        appendField(snapshot.country)
    }

    private fun StringBuilder.appendItems(items: List<OrderItemSnapshot>) {
        appendField(items.size.toString())
        items.forEach { item ->
            appendField(item.productId.toString())
            appendField(item.productName)
            appendField(item.unitPriceAmount.toPlainString())
            appendField(item.currency.name)
            appendField(item.quantity.toString())
        }
    }

    private fun StringBuilder.appendField(value: String?) {
        if (value == null) append("-1:") else append(value.length).append(':').append(value)
    }

    private fun digest(canonicalPayload: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(canonicalPayload.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
}

internal object OrderCreationPayloadValidator {
    fun validateBase(
        customerId: Long,
        customerSnapshot: CustomerSnapshot,
        shippingAddressSnapshot: ShippingAddressSnapshot,
        idempotencyKey: String,
    ) {
        positive(customerId, "customerId")
        required(idempotencyKey, "idempotencyKey")
        length(idempotencyKey, "idempotencyKey", 255)
        if (customerSnapshot.customerId != customerId) {
            invalid("customerSnapshot.customerId must match customerId.")
        }
        validateCustomer(customerSnapshot)
        validateShippingAddress(shippingAddressSnapshot)
    }

    fun validateCart(
        cartId: Long,
        items: List<OrderItemSnapshot>,
    ) {
        positive(cartId, "cartId")
        if (items.isEmpty()) invalid("cart items must not be empty.")
        validateItems(items)
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

    fun validateItems(items: List<OrderItemSnapshot>) {
        if (items.map { it.currency }.toSet().size != 1) invalid("cart items must use a single currency.")
        items.forEachIndexed { index, item ->
            positive(item.productId, "items[$index].productId")
            required(item.productName, "items[$index].productName")
            length(item.productName, "items[$index].productName", 220)
            if (item.unitPriceAmount.signum() < 0) invalid("items[$index].unitPriceAmount must not be negative.")
            val normalizedAmount = item.unitPriceAmount.stripTrailingZeros()
            val effectiveScale = normalizedAmount.scale().coerceAtLeast(0)
            val integerDigits = (normalizedAmount.precision() - normalizedAmount.scale()).coerceAtLeast(1)
            if (effectiveScale > 2 || integerDigits > 10) invalid("items[$index].unitPriceAmount exceeds NUMERIC(12, 2).")
            if (item.quantity <= 0) invalid("items[$index].quantity must be greater than zero.")
        }
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
