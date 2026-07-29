package com.nexus.shopping.notification

import com.nexus.shopping.notification.application.exception.NotificationNotFoundException
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.application.usecase.GetNotificationByIdUseCase
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationChannel
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.notification.domain.NotificationType
import com.nexus.shopping.platform.domain.PageResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetNotificationByIdUseCaseTest {
    private fun sampleNotification(id: Long) =
        Notification(
            id = id,
            customerId = 1L,
            notificationKey = "notification-$id",
            recipientEmail = "cliente@example.com",
            type = NotificationType.ORDER_CONFIRMED,
            channel = NotificationChannel.EMAIL,
            status = NotificationStatus.SENT,
            subject = "Pedido 123 confirmado",
            body = "Seu pedido 123 no valor de 99.90 foi confirmado.",
            referenceId = 123L,
            createdAt = Instant.parse("2026-07-17T12:00:00Z"),
            sentAt = Instant.parse("2026-07-17T12:00:00Z"),
        )

    @Test
    fun `returns notification when found`() {
        val notifications = mapOf(1L to sampleNotification(1L))
        val fakeRepo =
            object : NotificationRepositoryPort {
                override fun save(notification: Notification): Notification = throw UnsupportedOperationException()

                override fun reserve(notification: Notification): Notification = throw UnsupportedOperationException()

                override fun claim(
                    notificationKey: String,
                    sendingLeaseToken: String,
                    sendingLeaseUntil: Instant,
                    now: Instant,
                ): Notification? = throw UnsupportedOperationException()

                override fun complete(
                    notificationKey: String,
                    sendingLeaseToken: String,
                    status: NotificationStatus,
                    sentAt: Instant?,
                ): Notification? = throw UnsupportedOperationException()

                override fun findByNotificationKey(notificationKey: String): Notification? = throw UnsupportedOperationException()

                override fun findById(id: Long): Notification? = notifications[id]

                override fun findByCustomerId(
                    customerId: Long,
                    page: Int,
                    size: Int,
                ): PageResult<Notification> = throw UnsupportedOperationException()
            }
        val useCase = GetNotificationByIdUseCase(fakeRepo)

        val notification = useCase.execute(1L)

        assertEquals(1L, notification.id)
        assertEquals("Pedido 123 confirmado", notification.subject)
    }

    @Test
    fun `throws NotificationNotFoundException when not found`() {
        val fakeRepo =
            object : NotificationRepositoryPort {
                override fun save(notification: Notification): Notification = throw UnsupportedOperationException()

                override fun reserve(notification: Notification): Notification = throw UnsupportedOperationException()

                override fun claim(
                    notificationKey: String,
                    sendingLeaseToken: String,
                    sendingLeaseUntil: Instant,
                    now: Instant,
                ): Notification? = throw UnsupportedOperationException()

                override fun complete(
                    notificationKey: String,
                    sendingLeaseToken: String,
                    status: NotificationStatus,
                    sentAt: Instant?,
                ): Notification? = throw UnsupportedOperationException()

                override fun findByNotificationKey(notificationKey: String): Notification? = throw UnsupportedOperationException()

                override fun findById(id: Long): Notification? = null

                override fun findByCustomerId(
                    customerId: Long,
                    page: Int,
                    size: Int,
                ): PageResult<Notification> = throw UnsupportedOperationException()
            }
        val useCase = GetNotificationByIdUseCase(fakeRepo)

        assertFailsWith<NotificationNotFoundException> {
            useCase.execute(9999L)
        }
    }
}
