package com.nexus.shopping.payment.adapter.outbound.jpa

import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptRepositoryPort
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptReservation
import com.nexus.shopping.payment.domain.PaymentAttempt
import com.nexus.shopping.payment.domain.PaymentStatus
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

@Repository
class PaymentJpaRepositoryAdapter(
    private val repository: SpringDataPaymentAttemptRepository,
    transactionManager: PlatformTransactionManager,
) : PaymentAttemptRepositoryPort {
    private val transactions =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    override fun reserve(attempt: PaymentAttempt): PaymentAttemptReservation =
        try {
            reserveInNewTransaction(attempt)
        } catch (exception: DataIntegrityViolationException) {
            val winner = findByIdempotencyKeyInNewTransaction(attempt)
            if (winner != null) PaymentAttemptReservation.Existing(winner) else throw exception
        }

    override fun complete(
        attemptReference: String,
        processingLeaseToken: String,
        status: PaymentStatus,
        providerTransactionId: String?,
        completedAt: Instant,
    ): PaymentAttempt? {
        val completed =
            requireNotNull(
                transactions.execute {
                    repository.completeIfCurrentLeaseToken(
                        attemptReference = attemptReference,
                        processingLeaseToken = processingLeaseToken,
                        status = status,
                        providerTransactionId = providerTransactionId,
                        completedAt = completedAt,
                    ) == 1
                },
            )
        if (!completed) return null
        return requireNotNull(
            transactions.execute {
                repository.findByAttemptReference(attemptReference).orElse(null)?.toDomain()
            },
        )
    }

    private fun reserveInNewTransaction(attempt: PaymentAttempt): PaymentAttemptReservation =
        requireNotNull(
            transactions.execute {
                val existing = repository.findByReferenceIdAndIdempotencyKey(attempt.referenceId, attempt.idempotencyKey).orElse(null)
                if (existing != null) {
                    val reclaimed =
                        existing.authorizationFingerprint == attempt.authorizationFingerprint &&
                            repository.reclaimExpiredLease(
                                referenceId = attempt.referenceId,
                                idempotencyKey = attempt.idempotencyKey,
                                processingLeaseToken = requireNotNull(attempt.processingLeaseToken),
                                processingLeaseUntil = requireNotNull(attempt.processingLeaseUntil),
                                now = Instant.now(),
                            ) == 1
                    val winner =
                        requireNotNull(
                            repository
                                .findByReferenceIdAndIdempotencyKey(attempt.referenceId, attempt.idempotencyKey)
                                .orElse(null),
                        )
                    if (reclaimed) {
                        PaymentAttemptReservation.Created(winner.toDomain())
                    } else {
                        PaymentAttemptReservation.Existing(winner.toDomain())
                    }
                } else {
                    PaymentAttemptReservation.Created(repository.saveAndFlush(attempt.toEntity()).toDomain())
                }
            },
        )

    private fun findByIdempotencyKeyInNewTransaction(attempt: PaymentAttempt): PaymentAttempt? =
        requireNotNull(
            transactions.execute {
                repository.findByReferenceIdAndIdempotencyKey(attempt.referenceId, attempt.idempotencyKey).orElse(null)?.toDomain()
            },
        )
}
