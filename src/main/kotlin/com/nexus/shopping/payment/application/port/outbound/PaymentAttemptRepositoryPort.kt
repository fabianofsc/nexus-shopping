package com.nexus.shopping.payment.application.port.outbound

import com.nexus.shopping.payment.domain.PaymentAttempt
import com.nexus.shopping.payment.domain.PaymentStatus
import java.time.Instant

interface PaymentAttemptRepositoryPort {
    fun reserve(attempt: PaymentAttempt): PaymentAttemptReservation

    fun findByReferenceIdAndIdempotencyKey(
        referenceId: String,
        idempotencyKey: String,
    ): PaymentAttempt?

    fun complete(
        attemptReference: String,
        processingLeaseToken: String,
        status: PaymentStatus,
        providerTransactionId: String?,
        completedAt: Instant,
    ): PaymentAttempt?
}

sealed interface PaymentAttemptReservation {
    data class Created(
        val attempt: PaymentAttempt,
    ) : PaymentAttemptReservation

    data class Existing(
        val attempt: PaymentAttempt,
    ) : PaymentAttemptReservation
}
