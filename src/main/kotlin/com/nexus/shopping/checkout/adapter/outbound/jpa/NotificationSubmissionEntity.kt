package com.nexus.shopping.checkout.adapter.outbound.jpa

import com.nexus.shopping.checkout.application.model.NotificationSubmission
import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "notification_submissions")
class NotificationSubmissionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "order_id", nullable = false)
    var orderId: Long = 0,
    @Column(name = "customer_id", nullable = false)
    var customerId: Long = 0,
    @Column(name = "attempt_reference", nullable = false, length = 255)
    var attemptReference: String = "",
    @Column(name = "recipient_email", nullable = false, length = 254)
    var recipientEmail: String = "",
    @Column(name = "notification_key", nullable = false, length = 255)
    var notificationKey: String = "",
    @Column(name = "reference_id", nullable = false, length = 255)
    var referenceId: String = "",
    @Column(name = "subject", nullable = false, length = 180)
    var subject: String = "",
    @Column(name = "body", nullable = false, length = 2000)
    var body: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: NotificationSubmissionStatus = NotificationSubmissionStatus.PENDING,
    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0,
    @Column(name = "last_error", length = 2000)
    var lastError: String? = null,
    @Column(name = "notification_id", length = 255)
    var notificationId: String? = null,
    @Column(name = "discard_reason", length = 500)
    var discardReason: String? = null,
    @Column(name = "sending_lease_until")
    var sendingLeaseUntil: Instant? = null,
    @Column(name = "sending_lease_token", length = 255)
    var sendingLeaseToken: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
) {
    fun toDomain(): NotificationSubmission =
        NotificationSubmission(
            id = requireNotNull(id) { "NotificationSubmissionEntity.id must be available before mapping to domain." },
            orderId = orderId,
            customerId = customerId,
            attemptReference = attemptReference,
            recipientEmail = recipientEmail,
            notificationKey = notificationKey,
            referenceId = referenceId,
            subject = subject,
            body = body,
            status = status,
            attemptCount = attemptCount,
            lastError = lastError,
            notificationId = notificationId,
            discardReason = discardReason,
            sendingLeaseUntil = sendingLeaseUntil,
            sendingLeaseToken = sendingLeaseToken,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}

fun NotificationSubmission.toEntity(now: Instant): NotificationSubmissionEntity =
    NotificationSubmissionEntity(
        id = id,
        orderId = orderId,
        customerId = customerId,
        attemptReference = attemptReference,
        recipientEmail = recipientEmail,
        notificationKey = notificationKey,
        referenceId = referenceId,
        subject = subject,
        body = body,
        status = status,
        attemptCount = attemptCount,
        lastError = lastError,
        notificationId = notificationId,
        discardReason = discardReason,
        sendingLeaseUntil = sendingLeaseUntil,
        sendingLeaseToken = sendingLeaseToken,
        createdAt = createdAt ?: now,
        updatedAt = updatedAt ?: now,
    )
