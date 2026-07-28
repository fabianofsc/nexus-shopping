package com.nexus.shopping.notification.application.port.outbound

import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.platform.domain.PageResult
import java.time.Instant

interface NotificationRepositoryPort {
    fun save(notification: Notification): Notification

    fun reserve(notification: Notification): Notification

    fun claim(
        notificationKey: String,
        sendingLeaseToken: String,
        sendingLeaseUntil: Instant,
        now: Instant,
    ): Notification?

    fun complete(
        notificationKey: String,
        sendingLeaseToken: String,
        status: NotificationStatus,
        sentAt: Instant?,
    ): Notification?

    fun findByNotificationKey(notificationKey: String): Notification?

    fun findById(id: Long): Notification?

    fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Notification>
}
