package com.nexus.shopping.notification.adapter.outbound.jpa

import com.nexus.shopping.notification.domain.NotificationChannel
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.notification.domain.NotificationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NotificationEntityTest {
    @Test
    fun `toDomain maps all fields`() {
        val entity =
            NotificationEntity(
                id = 10L,
                customerId = 1L,
                recipientEmail = "cliente@example.com",
                type = NotificationType.ORDER_CONFIRMED,
                channel = NotificationChannel.EMAIL,
                status = NotificationStatus.SENT,
                subject = "Pedido 123 confirmado",
                body = "Seu pedido 123 no valor de 99.90 foi confirmado.",
                referenceId = 123L,
                createdAt = java.time.Instant.parse("2026-07-17T12:00:00Z"),
                sentAt = java.time.Instant.parse("2026-07-17T12:00:00Z"),
            )

        val notification = entity.toDomain()

        assertEquals(10L, notification.id)
        assertEquals(1L, notification.customerId)
        assertEquals(NotificationStatus.SENT, notification.status)
        assertEquals(123L, notification.referenceId)
    }

    @Test
    fun `toDomain throws when id is missing`() {
        val entity =
            NotificationEntity(
                id = null,
                customerId = 1L,
                recipientEmail = "cliente@example.com",
                type = NotificationType.ORDER_CONFIRMED,
                channel = NotificationChannel.EMAIL,
                status = NotificationStatus.SENT,
                subject = "Pedido 123 confirmado",
                body = "Seu pedido 123 no valor de 99.90 foi confirmado.",
                referenceId = null,
                createdAt = java.time.Instant.parse("2026-07-17T12:00:00Z"),
                sentAt = null,
            )

        assertFailsWith<IllegalArgumentException> {
            entity.toDomain()
        }
    }
}
