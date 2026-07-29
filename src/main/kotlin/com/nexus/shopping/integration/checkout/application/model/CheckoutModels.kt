package com.nexus.shopping.integration.checkout.application.model

import java.math.BigDecimal
import java.time.Instant

data class CheckoutCommand(
    val customerId: Long,
    val customerSnapshot: CheckoutCustomerSnapshot,
    val shippingAddressSnapshot: CheckoutShippingAddressSnapshot,
    val paymentToken: String,
    val idempotencyKey: String,
) {
    override fun toString(): String =
        "CheckoutCommand(customerId=$customerId, customerSnapshot=$customerSnapshot, " +
            "shippingAddressSnapshot=$shippingAddressSnapshot, paymentToken=<redacted>, idempotencyKey=$idempotencyKey)"
}

data class CheckoutCustomerSnapshot(
    val customerId: Long,
    val name: String,
    val document: String,
    val documentType: String,
    val email: String,
    val phone: String?,
)

data class CheckoutShippingAddressSnapshot(
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
)

data class CheckoutItemSnapshot(
    val productId: Long,
    val productName: String,
    val unitPriceAmount: BigDecimal,
    val currency: String,
    val quantity: Int,
) {
    val totalAmount: BigDecimal
        get() = unitPriceAmount.multiply(BigDecimal.valueOf(quantity.toLong()))
}

data class CheckoutCartSnapshot(
    val reservationId: Long,
    val customerId: Long,
    val items: List<CheckoutItemSnapshot>,
) {
    val totalAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { total, item -> total + item.totalAmount }
}

data class PaymentAuthorizationCommand(
    val paymentToken: String,
    val idempotencyKey: String,
) {
    override fun toString(): String = "PaymentAuthorizationCommand(paymentToken=<redacted>, idempotencyKey=$idempotencyKey)"
}

data class FindCheckoutOrderReplayCommand(
    val customerId: Long,
    val customerSnapshot: CheckoutCustomerSnapshot,
    val shippingAddressSnapshot: CheckoutShippingAddressSnapshot,
    val idempotencyKey: String,
    val paymentAuthorizationFingerprint: String,
)

data class CreateCheckoutOrderCommand(
    val customerId: Long,
    val cartId: Long,
    val customerSnapshot: CheckoutCustomerSnapshot,
    val shippingAddressSnapshot: CheckoutShippingAddressSnapshot,
    val items: List<CheckoutItemSnapshot>,
    val idempotencyKey: String,
    val paymentAuthorizationFingerprint: String,
)

data class CheckoutOrderSnapshot(
    val id: Long,
    val orderReference: String,
    val customerId: Long,
    val cartId: Long,
    val recipientEmail: String,
    val customerSnapshot: CheckoutCustomerSnapshot,
    val shippingAddressSnapshot: CheckoutShippingAddressSnapshot,
    val items: List<CheckoutItemSnapshot>,
    val totalAmount: BigDecimal,
    val status: String,
    val awaitingPayment: Boolean,
    val createdAt: Instant,
    val cancelledAt: Instant?,
    val replayed: Boolean,
)

data class PaymentValidationCommand(
    val amount: BigDecimal,
    val currency: String,
)

data class PaymentProcessingCommand(
    val referenceId: String,
    val amount: BigDecimal,
    val currency: String,
    val paymentToken: String,
    val idempotencyKey: String,
) {
    override fun toString(): String =
        "PaymentProcessingCommand(referenceId=$referenceId, amount=$amount, currency=$currency, " +
            "paymentToken=<redacted>, idempotencyKey=$idempotencyKey)"
}

data class PaymentProcessingResult(
    val attemptReference: String,
    val status: PaymentResultStatus,
    val providerTransactionId: String?,
    val replayed: Boolean,
)

enum class PaymentResultStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
}

data class ApplyOrderPaymentResultCommand(
    val order: CheckoutOrderSnapshot,
    val payment: PaymentProcessingResult,
)

data class EnsureOrderConfirmationCommand(
    val orderId: Long,
    val customerId: Long,
    val recipientEmail: String,
    val amount: BigDecimal,
    val attemptReference: String,
)
