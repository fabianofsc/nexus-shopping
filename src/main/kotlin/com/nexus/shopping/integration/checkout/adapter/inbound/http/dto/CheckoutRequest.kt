package com.nexus.shopping.integration.checkout.adapter.inbound.http.dto

import com.nexus.shopping.integration.checkout.application.model.CheckoutCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutCustomerData
import com.nexus.shopping.integration.checkout.application.model.CheckoutShippingAddressData

data class CheckoutRequest(
    val customerSnapshot: CustomerSnapshotRequest,
    val shippingAddressSnapshot: ShippingAddressSnapshotRequest,
    val paymentToken: String,
) {
    override fun toString(): String =
        "CheckoutRequest(customerSnapshot=$customerSnapshot, shippingAddressSnapshot=$shippingAddressSnapshot, " +
            "paymentToken=<redacted>)"
}

data class CustomerSnapshotRequest(
    val name: String,
    val document: String,
    val documentType: String,
    val email: String,
    val phone: String?,
)

data class ShippingAddressSnapshotRequest(
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
)

fun CheckoutRequest.toCommand(
    customerId: Long,
    idempotencyKey: String,
) = CheckoutCommand(
    customerId = customerId,
    customerSnapshot =
        CheckoutCustomerData(
            customerId,
            customerSnapshot.name,
            customerSnapshot.document,
            customerSnapshot.documentType,
            customerSnapshot.email,
            customerSnapshot.phone,
        ),
    shippingAddressSnapshot =
        CheckoutShippingAddressData(
            shippingAddressSnapshot.street,
            shippingAddressSnapshot.number,
            shippingAddressSnapshot.complement,
            shippingAddressSnapshot.neighborhood,
            shippingAddressSnapshot.city,
            shippingAddressSnapshot.state,
            shippingAddressSnapshot.zipCode,
            shippingAddressSnapshot.country,
        ),
    paymentToken = paymentToken,
    idempotencyKey = idempotencyKey,
)
