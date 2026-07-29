package com.nexus.shopping.notification.domain

import java.time.Instant

data class Notification(
    val id: Long?,
    val customerId: Long,
    val notificationKey: String,
    val recipientEmail: String,
    val type: NotificationType,
    val channel: NotificationChannel,
    val status: NotificationStatus,
    val subject: String,
    val body: String,
    val referenceId: Long?,
    val createdAt: Instant?,
    val sentAt: Instant?,
    val sendingLeaseUntil: Instant? = null,
    val sendingLeaseToken: String? = null,
)
