package com.nexus.shopping.notification.application.usecase

import com.nexus.shopping.notification.application.command.SendNotificationCommand
import com.nexus.shopping.notification.application.exception.NotificationValidationException
import com.nexus.shopping.notification.application.port.outbound.EmailSenderPort
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationChannel
import com.nexus.shopping.notification.domain.NotificationMessageRenderer
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.notification.domain.NotificationType
import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SendNotificationUseCase(
    private val notificationRepository: NotificationRepositoryPort,
    private val emailSender: EmailSenderPort,
) {
    fun send(command: SendNotificationCommand): Notification {
        logger.infoWithContext(
            "notification.send.started",
            "notification.customer_id" to command.customerId,
            "notification.type" to command.type,
        )

        if (command.customerId <= 0) throwValidationFailed("customerId must be greater than 0.")
        if (command.recipientEmail.isBlank()) throwValidationFailed("recipientEmail must not be blank.")
        if (command.recipientEmail.length > 254) throwValidationFailed("recipientEmail must be at most 254 characters.")
        if (!command.recipientEmail.contains("@")) throwValidationFailed("recipientEmail must be valid.")

        val type = requireValidType(command.type)

        val missingParams = NotificationMessageRenderer.requiredPlaceholders(type) - command.templateParams.keys
        if (missingParams.isNotEmpty()) {
            throwValidationFailed(
                "Missing required templateParams for type ${type.name}: ${missingParams.sorted().joinToString(", ")}.",
            )
        }

        val message = NotificationMessageRenderer.render(type, command.templateParams)
        if (message.subject.length > 180) throwValidationFailed("subject exceeds maximum length of 180 characters.")
        if (message.body.length > 2000) throwValidationFailed("body exceeds maximum length of 2000 characters.")

        val result = emailSender.send(command.recipientEmail, message.subject, message.body)
        if (!result.success) {
            logger.warnWithContext(
                "notification.send.email_failed",
                "notification.customer_id" to command.customerId,
                "notification.failure_reason" to (result.failureReason ?: "unknown"),
            )
        }

        val notification =
            Notification(
                id = null,
                customerId = command.customerId,
                recipientEmail = command.recipientEmail,
                type = type,
                channel = NotificationChannel.EMAIL,
                status = if (result.success) NotificationStatus.SENT else NotificationStatus.FAILED,
                subject = message.subject,
                body = message.body,
                referenceId = command.referenceId,
                createdAt = null,
                sentAt = if (result.success) Instant.now() else null,
            )

        val saved = notificationRepository.save(notification)
        logger.infoWithContext(
            "notification.send.completed",
            "notification.id" to saved.id,
            "notification.status" to saved.status,
        )
        return saved
    }

    private fun requireValidType(type: String): NotificationType {
        val validNames = NotificationType.entries.map { it.name }
        if (type !in validNames) {
            throwValidationFailed("type must be one of: ${validNames.joinToString(", ")}.")
        }
        return NotificationType.valueOf(type)
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("notification.send.validation_failed", "validation.error" to message)
        throw NotificationValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SendNotificationUseCase::class.java)
    }
}
