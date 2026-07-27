package com.nexus.shopping.integration.checkout.application.model

import java.math.BigDecimal
import java.time.Instant

data class CheckoutCommand(
    val customerId: Long,
    val customerSnapshot: CheckoutCustomerData,
    val shippingAddressSnapshot: CheckoutShippingAddressData,
    val idempotencyKey: String,
)

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
)

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
