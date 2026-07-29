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
    val lifecycle = mutableListOf<NotificationStatus>()
    var current: Notification? = null
    var failNextCompletion = false
    private var nextId = 1L

    override fun save(notification: Notification): Notification {
        val persisted = notification.copy(id = nextId++, createdAt = Instant.parse("2026-07-17T12:00:00Z"))
        current = persisted
        return persisted
    }

    override fun reserve(notification: Notification): Notification {
        current?.takeIf { it.notificationKey == notification.notificationKey }?.let { return it }
        val persisted = save(notification)
        lifecycle += persisted.status
        return persisted
    }

    override fun claim(
        notificationKey: String,
        sendingLeaseToken: String,
        sendingLeaseUntil: Instant,
        now: Instant,
    ): Notification? {
        val notification = current ?: return null
        if (notification.notificationKey != notificationKey) return null
        val claimable =
            notification.status == NotificationStatus.PENDING ||
                notification.status == NotificationStatus.FAILED ||
                notification.status == NotificationStatus.SENDING &&
                requireNotNull(notification.sendingLeaseUntil).isBefore(now)
        if (!claimable) return null
        return notification
            .copy(
                status = NotificationStatus.SENDING,
                sendingLeaseToken = sendingLeaseToken,
                sendingLeaseUntil = sendingLeaseUntil,
            ).also {
                current = it
                lifecycle += it.status
            }
    }

    override fun complete(
        notificationKey: String,
        sendingLeaseToken: String,
        status: NotificationStatus,
        sentAt: Instant?,
    ): Notification? {
        if (failNextCompletion) {
            failNextCompletion = false
            error("database unavailable after external send")
        }
        val notification = current ?: return null
        if (notification.notificationKey != notificationKey ||
            notification.status != NotificationStatus.SENDING ||
            notification.sendingLeaseToken != sendingLeaseToken
        ) {
            return null
        }
        return notification
            .copy(
                status = status,
                sendingLeaseToken = null,
                sendingLeaseUntil = null,
                sentAt = sentAt,
            ).also {
                current = it
                lifecycle += it.status
            }
    }

    override fun findByNotificationKey(notificationKey: String): Notification? = current?.takeIf { it.notificationKey == notificationKey }

    override fun findById(id: Long): Notification? = current?.takeIf { it.id == id }

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Notification> = throw UnsupportedOperationException()
}

private class FakeEmailSender(
    var result: EmailSendResult = EmailSendResult(success = true),
) : EmailSenderPort {
    var failure: RuntimeException? = null
    var lastTo: String? = null
    var lastSubject: String? = null
    var lastBody: String? = null
    var sends = 0

    override fun send(
        to: String,
        subject: String,
        body: String,
    ): EmailSendResult {
        sends += 1
        failure?.let { throw it }
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
            notificationKey = "order-confirmed:123:pay_attempt_1",
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
        assertEquals(
            listOf(NotificationStatus.PENDING, NotificationStatus.SENDING, NotificationStatus.SENT),
            repository.lifecycle,
        )
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
        assertEquals(
            listOf(NotificationStatus.PENDING, NotificationStatus.SENDING, NotificationStatus.FAILED),
            repository.lifecycle,
        )
    }

    @Test
    fun `does not send again when notification key is already SENT`() {
        val repository = FakeNotificationRepository()
        val emailSender = FakeEmailSender()
        val useCase = SendNotificationUseCase(repository, emailSender)

        val first = useCase.send(validCommand())
        val replay = useCase.send(validCommand())

        assertEquals(first, replay)
        assertEquals(1, emailSender.sends)
    }

    @Test
    fun `reclaims a FAILED notification and retries delivery`() {
        val repository = FakeNotificationRepository()
        val emailSender = FakeEmailSender(EmailSendResult(success = false, failureReason = "smtp down"))
        val useCase = SendNotificationUseCase(repository, emailSender)
        assertEquals(NotificationStatus.FAILED, useCase.send(validCommand()).status)
        emailSender.result = EmailSendResult(success = true)

        val retried = useCase.send(validCommand())

        assertEquals(NotificationStatus.SENT, retried.status)
        assertEquals(2, emailSender.sends)
    }

    @Test
    fun `marks notification FAILED when sender throws before confirming delivery`() {
        val repository = FakeNotificationRepository()
        val emailSender = FakeEmailSender().apply { failure = IllegalStateException("smtp unavailable") }
        val useCase = SendNotificationUseCase(repository, emailSender)

        assertFailsWith<IllegalStateException> { useCase.send(validCommand()) }

        assertEquals(NotificationStatus.FAILED, repository.current?.status)
        assertEquals(null, repository.current?.sendingLeaseToken)
        assertEquals(null, repository.current?.sendingLeaseUntil)
    }

    @Test
    fun `does not send while another owner holds a valid SENDING lease`() {
        val repository = FakeNotificationRepository()
        val emailSender = FakeEmailSender()
        repository.current =
            Notification(
                id = 1L,
                customerId = 1L,
                notificationKey = validCommand().notificationKey,
                recipientEmail = "cliente@example.com",
                type = com.nexus.shopping.notification.domain.NotificationType.ORDER_CONFIRMED,
                channel = com.nexus.shopping.notification.domain.NotificationChannel.EMAIL,
                status = NotificationStatus.SENDING,
                subject = "Pedido 123 confirmado",
                body = "Seu pedido 123 no valor de 99.90 foi confirmado.",
                referenceId = 123L,
                createdAt = Instant.parse("2026-07-17T12:00:00Z"),
                sentAt = null,
                sendingLeaseUntil = Instant.now().plusSeconds(30),
                sendingLeaseToken = "current-owner",
            )

        val result = SendNotificationUseCase(repository, emailSender).send(validCommand())

        assertEquals(NotificationStatus.SENDING, result.status)
        assertEquals(0, emailSender.sends)
    }

    @Test
    fun `reclaim after failure to persist SENT may repeat external delivery`() {
        val repository = FakeNotificationRepository()
        val emailSender = FakeEmailSender()
        val useCase = SendNotificationUseCase(repository, emailSender)
        repository.failNextCompletion = true

        assertFailsWith<IllegalStateException> { useCase.send(validCommand()) }
        assertEquals(1, emailSender.sends)
        repository.current =
            requireNotNull(repository.current).copy(
                sendingLeaseUntil = Instant.now().minusSeconds(1),
            )

        val reclaimed = useCase.send(validCommand())

        assertEquals(NotificationStatus.SENT, reclaimed.status)
        assertEquals(2, emailSender.sends)
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
