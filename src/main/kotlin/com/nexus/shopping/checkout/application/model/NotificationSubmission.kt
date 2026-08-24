package com.nexus.shopping.checkout.application.model

import com.nexus.shopping.checkout.application.exception.CheckoutValidationException
import java.time.Instant

data class NotificationSubmission(
    val id: Long? = null,
    val orderId: Long,
    val customerId: Long,
    val attemptReference: String,
    val recipientEmail: String,
    val notificationKey: String,
    val referenceId: String,
    val subject: String,
    val body: String,
    val status: NotificationSubmissionStatus = NotificationSubmissionStatus.PENDING,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val notificationId: String? = null,
    val discardReason: String? = null,
    val sendingLeaseUntil: Instant? = null,
    val sendingLeaseToken: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
) {
    fun discard(reason: String): NotificationSubmission {
        if (reason.trim().length !in 1..500) {
            throw CheckoutValidationException("discard reason must contain between 1 and 500 characters.")
        }
        if (status !in setOf(NotificationSubmissionStatus.PENDING, NotificationSubmissionStatus.FAILED)) {
            throw CheckoutValidationException("only pending or failed notification submissions can be discarded.")
        }
        return copy(
            status = NotificationSubmissionStatus.DISCARDED,
            discardReason = reason,
            sendingLeaseUntil = null,
            sendingLeaseToken = null,
        )
    }

    companion object {
        fun forOrderConfirmation(command: EnsureOrderConfirmationCommand): NotificationSubmission {
            val orderId = command.orderId.toString()
            val amount = command.amount.toPlainString()
            return NotificationSubmission(
                orderId = command.orderId,
                customerId = command.customerId,
                attemptReference = command.attemptReference,
                recipientEmail = command.recipientEmail,
                notificationKey = "order-confirmed:$orderId:${command.attemptReference}",
                referenceId = "order:$orderId",
                subject = "Pedido $orderId confirmado",
                body = "Seu pedido $orderId no valor de $amount foi confirmado.",
            )
        }
    }
}

enum class NotificationSubmissionStatus {
    PENDING,
    IN_FLIGHT,
    ACCEPTED,
    FAILED,
    DISCARDED,
}

data class NotificationSubmissionSummary(
    val id: Long,
    val orderId: Long,
    val notificationKey: String,
    val status: NotificationSubmissionStatus,
    val attemptCount: Int,
    val lastError: String?,
    val notificationId: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

data class DiscardNotificationSubmissionCommand(
    val submissionId: Long,
    val reason: String,
)
