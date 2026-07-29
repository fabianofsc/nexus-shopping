package com.nexus.shopping.notification.adapter.outbound.jpa

import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationChannel
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.notification.domain.NotificationType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SourceType
import java.time.Instant

@Entity
@Table(name = "notifications")
class NotificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "customer_id", nullable = false)
    var customerId: Long = 0,
    @Column(name = "recipient_email", nullable = false, length = 254)
    var recipientEmail: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    var type: NotificationType = NotificationType.ORDER_CONFIRMED,
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    var channel: NotificationChannel = NotificationChannel.EMAIL,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: NotificationStatus = NotificationStatus.SENT,
    @Column(name = "subject", nullable = false, length = 180)
    var subject: String = "",
    @Column(name = "body", nullable = false, length = 2000)
    var body: String = "",
    @Column(name = "reference_id")
    var referenceId: Long? = null,
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @Column(name = "sent_at")
    var sentAt: Instant? = null,
    @Column(name = "notification_key", nullable = false, unique = true, length = 255)
    var notificationKey: String = "",
    @Column(name = "sending_lease_until")
    var sendingLeaseUntil: Instant? = null,
    @Column(name = "sending_lease_token", length = 64)
    var sendingLeaseToken: String? = null,
) {
    fun toDomain(): Notification =
        Notification(
            id = requireNotNull(id) { "NotificationEntity.id must be available before mapping to domain." },
            customerId = customerId,
            recipientEmail = recipientEmail,
            type = type,
            channel = channel,
            status = status,
            subject = subject,
            body = body,
            referenceId = referenceId,
            createdAt = requireNotNull(createdAt) { "NotificationEntity.createdAt must be available before mapping to domain." },
            sentAt = sentAt,
            notificationKey = notificationKey,
            sendingLeaseUntil = sendingLeaseUntil,
            sendingLeaseToken = sendingLeaseToken,
        )
}

fun Notification.toEntity(): NotificationEntity =
    NotificationEntity(
        customerId = customerId,
        recipientEmail = recipientEmail,
        type = type,
        channel = channel,
        status = status,
        subject = subject,
        body = body,
        referenceId = referenceId,
        sentAt = sentAt,
        notificationKey = notificationKey,
        sendingLeaseUntil = sendingLeaseUntil,
        sendingLeaseToken = sendingLeaseToken,
    )
