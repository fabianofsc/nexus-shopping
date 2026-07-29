package com.nexus.shopping.payment.application.command

import java.math.BigDecimal

data class ProcessPaymentCommand(
    val referenceId: String,
    val amount: BigDecimal,
    val currency: String,
    val paymentToken: String,
    val idempotencyKey: String,
) {
    override fun toString(): String =
        "ProcessPaymentCommand(referenceId=$referenceId, amount=$amount, currency=$currency, paymentToken=<redacted>, idempotencyKey=$idempotencyKey)"
}
