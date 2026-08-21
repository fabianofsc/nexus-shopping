package com.nexus.shopping.checkout.adapter.outbound.jpa

import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional

interface SpringDataNotificationSubmissionRepository : JpaRepository<NotificationSubmissionEntity, Long> {
    @Query("SELECT n FROM NotificationSubmissionEntity n WHERE n.notificationKey = :notificationKey")
    fun findByNotificationKey(
        @Param("notificationKey") notificationKey: String,
    ): Optional<NotificationSubmissionEntity>

    @Query("SELECT n FROM NotificationSubmissionEntity n WHERE n.id = :submissionId")
    fun findSubmissionById(
        @Param("submissionId") submissionId: Long,
    ): Optional<NotificationSubmissionEntity>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE NotificationSubmissionEntity n
        SET n.status = com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus.IN_FLIGHT,
            n.attemptCount = n.attemptCount + 1,
            n.sendingLeaseToken = :sendingLeaseToken,
            n.sendingLeaseUntil = :sendingLeaseUntil,
            n.updatedAt = :now
        WHERE n.id = :submissionId
          AND (
              n.status = com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus.PENDING
              OR n.status = com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus.FAILED
              OR (
                  n.status = com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus.IN_FLIGHT
                  AND n.sendingLeaseUntil < :now
              )
          )
        """,
    )
    fun claimIfAvailable(
        @Param("submissionId") submissionId: Long,
        @Param("sendingLeaseToken") sendingLeaseToken: String,
        @Param("sendingLeaseUntil") sendingLeaseUntil: Instant,
        @Param("now") now: Instant,
    ): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE NotificationSubmissionEntity n
        SET n.status = com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus.ACCEPTED,
            n.notificationId = :notificationId,
            n.sendingLeaseToken = NULL,
            n.sendingLeaseUntil = NULL,
            n.updatedAt = :now
        WHERE n.id = :submissionId
          AND n.status = com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus.IN_FLIGHT
          AND n.sendingLeaseToken = :sendingLeaseToken
        """,
    )
    fun markAcceptedIfCurrentLease(
        @Param("submissionId") submissionId: Long,
        @Param("sendingLeaseToken") sendingLeaseToken: String,
        @Param("notificationId") notificationId: String,
        @Param("now") now: Instant,
    ): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE NotificationSubmissionEntity n
        SET n.status = com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus.FAILED,
            n.lastError = :lastError,
            n.sendingLeaseToken = NULL,
            n.sendingLeaseUntil = NULL,
            n.updatedAt = :now
        WHERE n.id = :submissionId
          AND n.status = com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus.IN_FLIGHT
          AND n.sendingLeaseToken = :sendingLeaseToken
        """,
    )
    fun markFailedIfCurrentLease(
        @Param("submissionId") submissionId: Long,
        @Param("sendingLeaseToken") sendingLeaseToken: String,
        @Param("lastError") lastError: String,
        @Param("now") now: Instant,
    ): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE NotificationSubmissionEntity n
        SET n.status = com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus.DISCARDED,
            n.discardReason = :reason,
            n.sendingLeaseToken = NULL,
            n.sendingLeaseUntil = NULL,
            n.updatedAt = :now
        WHERE n.id = :submissionId
          AND (
              n.status = com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus.PENDING
              OR n.status = com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus.FAILED
          )
        """,
    )
    fun discardIfAllowed(
        @Param("submissionId") submissionId: Long,
        @Param("reason") reason: String,
        @Param("now") now: Instant,
    ): Int

    @Query(
        """
        SELECT n FROM NotificationSubmissionEntity n
        WHERE :status IS NULL OR n.status = :status
        ORDER BY n.createdAt ASC, n.id ASC
        """,
    )
    fun findPage(
        @Param("status") status: NotificationSubmissionStatus?,
        pageable: Pageable,
    ): Slice<NotificationSubmissionEntity>
}
