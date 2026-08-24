package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.NotificationSubmission

interface NotificationServiceClientPort {
    @Throws(NotificationServiceUnavailableException::class, NotificationServiceRejectedException::class)
    fun accept(submission: NotificationSubmission): AcceptedNotification
}

data class AcceptedNotification(
    val notificationId: String,
)

class NotificationServiceUnavailableException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class NotificationServiceRejectedException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
