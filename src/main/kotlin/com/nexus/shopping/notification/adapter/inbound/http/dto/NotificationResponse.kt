package com.nexus.shopping.notification.adapter.inbound.http.dto

import com.nexus.shopping.notification.domain.Notification
import java.time.Instant

data class NotificationResponse(
    val id: Long,
    val customerId: Long,
    val recipientEmail: String,
    val type: String,
    val channel: String,
    val status: String,
    val subject: String,
    val body: String,
    val referenceId: Long?,
    val createdAt: Instant,
    val sentAt: Instant?,
)

fun Notification.toResponse(): NotificationResponse =
    NotificationResponse(
        id = requireNotNull(id) { "Notification.id must be available before mapping to response." },
        customerId = customerId,
        recipientEmail = recipientEmail,
        type = type.name,
        channel = channel.name,
        status = status.name,
        subject = subject,
        body = body,
        referenceId = referenceId,
        createdAt = requireNotNull(createdAt) { "Notification.createdAt must be available before mapping to response." },
        sentAt = sentAt,
    )
