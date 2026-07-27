package com.nexus.shopping.payment.domain

import java.time.Instant

@ConsistentCopyVisibility
data class PaymentAttempt private constructor(
    val id: Long?,
    val attemptReference: String,
    val referenceId: String,
    val amount: PaymentAmount,
    val currency: PaymentCurrency,
    val status: PaymentStatus,
    val provider: PaymentProvider,
    val providerTransactionId: String?,
    val idempotencyKey: String,
    val authorizationFingerprint: String,
    val processingLeaseUntil: Instant?,
    val processingLeaseToken: String?,
    val createdAt: Instant?,
    val completedAt: Instant?,
) {
    fun complete(
        resultStatus: PaymentStatus,
        resultProviderTransactionId: String?,
        completedAt: Instant,
    ): PaymentAttempt {
        if (status != PaymentStatus.REQUESTED) {
            throw PaymentDomainValidationException("only requested payment attempts can be completed.")
        }
        if (resultStatus == PaymentStatus.REQUESTED) {
            throw PaymentDomainValidationException("payment completion status must be terminal.")
        }
        return copy(
            status = resultStatus,
            providerTransactionId = resultProviderTransactionId,
            processingLeaseUntil = null,
            processingLeaseToken = null,
            completedAt = completedAt,
        )
    }

    companion object {
        fun restored(
            id: Long,
            attemptReference: String,
            referenceId: String,
            amount: PaymentAmount,
            currency: PaymentCurrency,
            status: PaymentStatus,
            provider: PaymentProvider,
            providerTransactionId: String?,
            idempotencyKey: String,
            authorizationFingerprint: String,
            processingLeaseUntil: Instant?,
            processingLeaseToken: String?,
            createdAt: Instant,
            completedAt: Instant?,
        ): PaymentAttempt =
            PaymentAttempt(
                id = id,
                attemptReference = attemptReference,
                referenceId = referenceId,
                amount = amount,
                currency = currency,
                status = status,
                provider = provider,
                providerTransactionId = providerTransactionId,
                idempotencyKey = idempotencyKey,
                authorizationFingerprint = authorizationFingerprint,
                processingLeaseUntil = processingLeaseUntil,
                processingLeaseToken = processingLeaseToken,
                createdAt = createdAt,
                completedAt = completedAt,
            )

        fun requested(
            attemptReference: String,
            referenceId: String,
            amount: PaymentAmount,
            currency: PaymentCurrency,
            provider: PaymentProvider,
            idempotencyKey: String,
            authorizationFingerprint: String,
            processingLeaseToken: String,
            processingLeaseUntil: Instant,
            createdAt: Instant,
        ): PaymentAttempt =
            PaymentAttempt(
                id = null,
                attemptReference = attemptReference,
                referenceId = referenceId,
                amount = amount,
                currency = currency,
                status = PaymentStatus.REQUESTED,
                provider = provider,
                providerTransactionId = null,
                idempotencyKey = idempotencyKey,
                authorizationFingerprint = authorizationFingerprint,
                processingLeaseUntil = processingLeaseUntil,
                processingLeaseToken = processingLeaseToken,
                createdAt = createdAt,
                completedAt = null,
            )
    }
}
