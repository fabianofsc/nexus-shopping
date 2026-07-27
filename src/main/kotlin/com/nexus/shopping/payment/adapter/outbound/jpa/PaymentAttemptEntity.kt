package com.nexus.shopping.payment.adapter.outbound.jpa

import com.nexus.shopping.payment.domain.PaymentAmount
import com.nexus.shopping.payment.domain.PaymentAttempt
import com.nexus.shopping.payment.domain.PaymentCurrency
import com.nexus.shopping.payment.domain.PaymentProvider
import com.nexus.shopping.payment.domain.PaymentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "payment_attempts")
class PaymentAttemptEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "attempt_reference", nullable = false, length = 255)
    var attemptReference: String = "",
    @Column(name = "reference_id", nullable = false, length = 255)
    var referenceId: String = "",
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: PaymentStatus = PaymentStatus.REQUESTED,
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 64)
    var provider: PaymentProvider = PaymentProvider.LOGGING_PROVIDER,
    @Column(name = "provider_transaction_id", length = 255)
    var providerTransactionId: String? = null,
    @Column(name = "idempotency_key", nullable = false, length = 255)
    var idempotencyKey: String = "",
    @Column(name = "authorization_fingerprint", nullable = false, length = 64)
    var authorizationFingerprint: String = "",
    @Column(name = "processing_lease_until")
    var processingLeaseUntil: Instant? = null,
    @Column(name = "processing_lease_token", length = 255)
    var processingLeaseToken: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @Column(name = "completed_at")
    var completedAt: Instant? = null,
) {
    fun toDomain(): PaymentAttempt =
        PaymentAttempt.restored(
            id = requireNotNull(id) { "PaymentAttemptEntity.id must be available before mapping to domain." },
            attemptReference = attemptReference,
            referenceId = referenceId,
            amount = PaymentAmount.of(amount),
            currency = PaymentCurrency.of(currency),
            status = status,
            provider = provider,
            providerTransactionId = providerTransactionId,
            idempotencyKey = idempotencyKey,
            authorizationFingerprint = authorizationFingerprint,
            processingLeaseUntil = processingLeaseUntil,
            processingLeaseToken = processingLeaseToken,
            createdAt = requireNotNull(createdAt) { "PaymentAttemptEntity.createdAt must be available before mapping to domain." },
            completedAt = completedAt,
        )
}

fun PaymentAttempt.toEntity(): PaymentAttemptEntity =
    PaymentAttemptEntity(
        id = id,
        attemptReference = attemptReference,
        referenceId = referenceId,
        amount = amount.value,
        currency = currency.code,
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
