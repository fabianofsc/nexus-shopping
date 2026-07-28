package com.nexus.shopping.notification.adapter.outbound.jpa

import com.nexus.shopping.notification.domain.NotificationStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional

interface SpringDataNotificationRepository : JpaRepository<NotificationEntity, Long> {
    @Query("SELECT n FROM NotificationEntity n WHERE n.notificationKey = :notificationKey")
    fun findByNotificationKey(
        @Param("notificationKey") notificationKey: String,
    ): Optional<NotificationEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE NotificationEntity n
        SET n.status = :sendingStatus,
            n.sendingLeaseToken = :sendingLeaseToken,
            n.sendingLeaseUntil = :sendingLeaseUntil
        WHERE n.notificationKey = :notificationKey
          AND (
            n.status IN :claimableStatuses
            OR (n.status = :sendingStatus AND n.sendingLeaseUntil < :now)
          )
        """,
    )
    fun claimForSending(
        @Param("notificationKey") notificationKey: String,
        @Param("sendingLeaseToken") sendingLeaseToken: String,
        @Param("sendingLeaseUntil") sendingLeaseUntil: Instant,
        @Param("now") now: Instant,
        @Param("sendingStatus") sendingStatus: NotificationStatus,
        @Param("claimableStatuses") claimableStatuses: Collection<NotificationStatus>,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE NotificationEntity n
        SET n.status = :status,
            n.sentAt = :sentAt,
            n.sendingLeaseToken = NULL,
            n.sendingLeaseUntil = NULL
        WHERE n.notificationKey = :notificationKey
          AND n.status = :sendingStatus
          AND n.sendingLeaseToken = :sendingLeaseToken
        """,
    )
    fun completeIfCurrentLeaseToken(
        @Param("notificationKey") notificationKey: String,
        @Param("sendingLeaseToken") sendingLeaseToken: String,
        @Param("sendingStatus") sendingStatus: NotificationStatus,
        @Param("status") status: NotificationStatus,
        @Param("sentAt") sentAt: Instant?,
    ): Int

    @Query(
        """
        SELECT n FROM NotificationEntity n
        WHERE n.customerId = :customerId
        ORDER BY n.id
        """,
    )
    fun findByCustomerId(
        @Param("customerId") customerId: Long,
        pageable: Pageable,
    ): Slice<NotificationEntity>
}
