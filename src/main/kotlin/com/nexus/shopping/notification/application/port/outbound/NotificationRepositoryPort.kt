package com.nexus.shopping.notification.application.port.outbound

import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationPage

interface NotificationRepositoryPort {
    fun save(notification: Notification): Notification

    fun findById(id: Long): Notification?

    fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): NotificationPage
}
