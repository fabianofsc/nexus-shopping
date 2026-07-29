package com.nexus.shopping.payment.domain

import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentAttemptTest {
    @Test
    fun `starts requested and transitions to approved`() {
        val requested = requestedAttempt()

        val approved = requested.complete(PaymentStatus.APPROVED, "provider-tx-1", Instant.parse("2026-07-26T12:01:00Z"))

        assertEquals(PaymentStatus.REQUESTED, requested.status)
        assertEquals(PaymentStatus.APPROVED, approved.status)
        assertEquals("provider-tx-1", approved.providerTransactionId)
    }

    @Test
    fun `starts requested and transitions to rejected`() {
        val rejected =
            requestedAttempt().complete(PaymentStatus.REJECTED, null, Instant.parse("2026-07-26T12:01:00Z"))

        assertEquals(PaymentStatus.REJECTED, rejected.status)
        assertEquals(null, rejected.providerTransactionId)
    }

    private fun requestedAttempt() =
        PaymentAttempt.requested(
            attemptReference = "pay_123",
            referenceId = "checkout:42",
            amount = PaymentAmount.of(BigDecimal("19.90")),
            currency = PaymentCurrency.of("BRL"),
            provider = PaymentProvider.LOGGING_PROVIDER,
            idempotencyKey = "checkout-key-1",
            authorizationFingerprint = "fingerprint",
            processingLeaseToken = "lease-1",
            processingLeaseUntil = Instant.parse("2026-07-26T12:05:00Z"),
            createdAt = Instant.parse("2026-07-26T12:00:00Z"),
        )
}
