package com.nexus.shopping.notification

import com.nexus.shopping.notification.application.command.SendNotificationCommand
import com.nexus.shopping.notification.application.exception.NotificationValidationException
import com.nexus.shopping.notification.application.port.outbound.EmailSendResult
import com.nexus.shopping.notification.application.port.outbound.EmailSenderPort
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.application.usecase.SendNotificationUseCase
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.platform.domain.PageResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

private class FakeNotificationRepository : NotificationRepositoryPort {
    val saved = mutableListOf<Notification>()
    private var nextId = 1L

    override fun save(notification: Notification): Notification {
        val persisted = notification.copy(id = nextId++, createdAt = Instant.parse("2026-07-17T12:00:00Z"))
        saved += persisted
        return persisted
    }

    override fun findById(id: Long): Notification? = saved.find { it.id == id }

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Notification> = throw UnsupportedOperationException()
}

private class FakeEmailSender(
    private val result: EmailSendResult = EmailSendResult(success = true),
) : EmailSenderPort {
    var lastTo: String? = null
    var lastSubject: String? = null
    var lastBody: String? = null

    override fun send(
        to: String,
        subject: String,
        body: String,
    ): EmailSendResult {
        lastTo = to
        lastSubject = subject
        lastBody = body
        return result
    }
}

class SendNotificationUseCaseTest {
    private fun validCommand() =
        SendNotificationCommand(
            customerId = 1L,
            recipientEmail = "cliente@example.com",
            type = "ORDER_CONFIRMED",
            referenceId = 123L,
            templateParams = mapOf("orderId" to "123", "amount" to "99.90"),
        )

    @Test
    fun `sends notification and persists it as SENT`() {
        val repository = FakeNotificationRepository()
        val emailSender = FakeEmailSender()
        val useCase = SendNotificationUseCase(repository, emailSender)

        val notification = useCase.send(validCommand())

        assertNotNull(notification.id)
        assertEquals(NotificationStatus.SENT, notification.status)
        assertEquals("Pedido 123 confirmado", notification.subject)
        assertEquals("Seu pedido 123 no valor de 99.90 foi confirmado.", notification.body)
        assertEquals("cliente@example.com", emailSender.lastTo)
        assertNotNull(notification.sentAt)
    }

    @Test
    fun `persists notification as FAILED when email sender reports failure`() {
        val repository = FakeNotificationRepository()
        val emailSender = FakeEmailSender(EmailSendResult(success = false, failureReason = "smtp down"))
        val useCase = SendNotificationUseCase(repository, emailSender)

        val notification = useCase.send(validCommand())

        assertEquals(NotificationStatus.FAILED, notification.status)
        assertEquals(null, notification.sentAt)
    }

    @Test
    fun `throws NotificationValidationException when customerId is not positive`() {
        val useCase = SendNotificationUseCase(FakeNotificationRepository(), FakeEmailSender())

        assertFailsWith<NotificationValidationException> {
            useCase.send(validCommand().copy(customerId = 0L))
        }
    }

    @Test
    fun `throws NotificationValidationException when recipientEmail is invalid`() {
        val useCase = SendNotificationUseCase(FakeNotificationRepository(), FakeEmailSender())

        assertFailsWith<NotificationValidationException> {
            useCase.send(validCommand().copy(recipientEmail = "invalid-email"))
        }
    }

    @Test
    fun `throws NotificationValidationException when type is invalid`() {
        val useCase = SendNotificationUseCase(FakeNotificationRepository(), FakeEmailSender())

        assertFailsWith<NotificationValidationException> {
            useCase.send(validCommand().copy(type = "ORDER_SHIPPED"))
        }
    }

    @Test
    fun `throws NotificationValidationException listing missing template params`() {
        val useCase = SendNotificationUseCase(FakeNotificationRepository(), FakeEmailSender())

        val exception =
            assertFailsWith<NotificationValidationException> {
                useCase.send(validCommand().copy(templateParams = mapOf("orderId" to "123")))
            }
        assertEquals(true, exception.message?.contains("amount"))
    }

    @Test
    fun `throws NotificationValidationException when rendered body exceeds 2000 characters`() {
        val useCase = SendNotificationUseCase(FakeNotificationRepository(), FakeEmailSender())
        val oversizedAmount = "9".repeat(2000)

        val exception =
            assertFailsWith<NotificationValidationException> {
                useCase.send(
                    validCommand().copy(
                        templateParams = mapOf("orderId" to "123", "amount" to oversizedAmount),
                    ),
                )
            }
        assertEquals(true, exception.message?.contains("body exceeds maximum length of 2000 characters"))
    }
}
