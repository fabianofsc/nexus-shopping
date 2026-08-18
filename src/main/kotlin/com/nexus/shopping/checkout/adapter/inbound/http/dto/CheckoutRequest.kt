package com.nexus.shopping.checkout.adapter.inbound.http.dto

import com.nexus.shopping.checkout.application.model.CheckoutCommand

data class CheckoutRequest(
    val paymentToken: String,
) {
    override fun toString(): String = "CheckoutRequest(paymentToken=<redacted>)"
}

fun CheckoutRequest.toCommand(
    customerId: Long,
    idempotencyKey: String,
) = CheckoutCommand(
    customerId = customerId,
    paymentToken = paymentToken,
    idempotencyKey = idempotencyKey,
)
