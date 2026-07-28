package com.nexus.shopping.payment.application.command

data class FingerprintPaymentAuthorizationCommand(
    val paymentToken: String,
    val idempotencyKey: String,
) {
    override fun toString(): String = "FingerprintPaymentAuthorizationCommand(paymentToken=<redacted>, idempotencyKey=$idempotencyKey)"
}
