package com.nexus.shopping.payment.adapter.outbound.jpa

import com.nexus.shopping.payment.domain.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional

interface SpringDataPaymentAttemptRepository : JpaRepository<PaymentAttemptEntity, Long> {
    @Query(
        """
        SELECT p FROM PaymentAttemptEntity p
        WHERE p.referenceId = :referenceId AND p.idempotencyKey = :idempotencyKey
        """,
    )
    fun findByReferenceIdAndIdempotencyKey(
        @Param("referenceId") referenceId: String,
        @Param("idempotencyKey") idempotencyKey: String,
    ): Optional<PaymentAttemptEntity>

    @Query("SELECT p FROM PaymentAttemptEntity p WHERE p.attemptReference = :attemptReference")
    fun findByAttemptReference(
        @Param("attemptReference") attemptReference: String,
    ): Optional<PaymentAttemptEntity>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE PaymentAttemptEntity p
        SET p.status = :status,
            p.providerTransactionId = :providerTransactionId,
            p.completedAt = :completedAt,
            p.processingLeaseUntil = NULL,
            p.processingLeaseToken = NULL
        WHERE p.attemptReference = :attemptReference
          AND p.processingLeaseToken = :processingLeaseToken
          AND p.status = com.nexus.shopping.payment.domain.PaymentStatus.REQUESTED
        """,
    )
    fun completeIfCurrentLeaseToken(
        @Param("attemptReference") attemptReference: String,
        @Param("processingLeaseToken") processingLeaseToken: String,
        @Param("status") status: PaymentStatus,
        @Param("providerTransactionId") providerTransactionId: String?,
        @Param("completedAt") completedAt: Instant,
    ): Int
}
