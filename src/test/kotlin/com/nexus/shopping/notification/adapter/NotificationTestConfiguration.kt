package com.nexus.shopping.notification.adapter

import com.nexus.shopping.notification.application.port.outbound.EmailSendResult
import com.nexus.shopping.notification.application.port.outbound.EmailSenderPort
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationPage
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Instant

@TestConfiguration
class NotificationTestConfiguration {
    @Bean
    @Primary
    fun testEmailSender(): EmailSenderPort = TestEmailSender()

    @Bean
    @Primary
    fun testNotificationRepository(): NotificationRepositoryPort = TestNotificationRepository()
}

private class TestEmailSender : EmailSenderPort {
    override fun send(
        to: String,
        subject: String,
        body: String,
    ): EmailSendResult = EmailSendResult(success = true)
}

private class TestNotificationRepository : NotificationRepositoryPort {
    override fun save(notification: Notification): Notification =
        notification.copy(id = 1L, createdAt = Instant.now())

    override fun findById(id: Long): Notification? = null

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): NotificationPage = NotificationPage(emptyList(), page, size, 0, false)
}
