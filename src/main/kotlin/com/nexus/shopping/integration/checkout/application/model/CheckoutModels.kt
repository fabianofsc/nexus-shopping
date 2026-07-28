package com.nexus.shopping.integration.checkout.application.model

import java.math.BigDecimal
import java.time.Instant

data class CheckoutCommand(
    val customerId: Long,
    val customerSnapshot: CheckoutCustomerData,
    val shippingAddressSnapshot: CheckoutShippingAddressData,
    val paymentToken: String,
    val idempotencyKey: String,
) {
    override fun toString(): String =
        "CheckoutCommand(customerId=$customerId, customerSnapshot=$customerSnapshot, " +
            "shippingAddressSnapshot=$shippingAddressSnapshot, paymentToken=<redacted>, idempotencyKey=$idempotencyKey)"
}

data class CheckoutCustomerData(
    val customerId: Long,
    val name: String,
    val document: String,
    val documentType: String,
    val email: String,
    val phone: String?,
)

data class CheckoutShippingAddressData(
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
)

data class CheckoutItemData(
    val productId: Long,
    val productName: String,
    val unitPriceAmount: BigDecimal,
    val currency: String,
    val quantity: Int,
) {
    val totalAmount: BigDecimal
        get() = unitPriceAmount.multiply(BigDecimal.valueOf(quantity.toLong()))
}

data class CheckoutCartData(
    val reservationId: Long,
    val customerId: Long,
    val items: List<CheckoutItemData>,
) {
    val totalAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { total, item -> total + item.totalAmount }
}

data class CreateOrderData(
    val customerId: Long,
    val cartId: Long,
    val customerSnapshot: CheckoutCustomerData,
    val shippingAddressSnapshot: CheckoutShippingAddressData,
    val items: List<CheckoutItemData>,
    val idempotencyKey: String,
)

data class CheckoutOrderData(
    val id: Long,
    val orderReference: String,
    val customerId: Long,
    val cartId: Long,
    val recipientEmail: String,
    val customerSnapshot: CheckoutCustomerData,
    val shippingAddressSnapshot: CheckoutShippingAddressData,
    val items: List<CheckoutItemData>,
    val totalAmount: BigDecimal,
    val status: String,
    val createdAt: Instant,
    val cancelledAt: Instant?,
    val replayed: Boolean,
)

data class PaymentValidationData(
    val amount: BigDecimal,
    val currency: String,
)

data class PaymentProcessingData(
    val referenceId: String,
    val amount: BigDecimal,
    val currency: String,
    val paymentToken: String,
    val idempotencyKey: String,
) {
    override fun toString(): String =
        "PaymentProcessingData(referenceId=$referenceId, amount=$amount, currency=$currency, " +
            "paymentToken=<redacted>, idempotencyKey=$idempotencyKey)"
}

data class PaymentResultData(
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

data class ApplyOrderPaymentResultData(
    val order: CheckoutOrderData,
    val payment: PaymentResultData,
)

data class OrderConfirmationNotificationData(
    val orderId: Long,
    val customerId: Long,
    val recipientEmail: String,
    val amount: BigDecimal,
    val attemptReference: String,
)
