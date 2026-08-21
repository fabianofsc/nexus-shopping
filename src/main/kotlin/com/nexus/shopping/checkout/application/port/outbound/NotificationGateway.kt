package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.EnsureOrderConfirmationCommand
import com.nexus.shopping.checkout.application.model.NotificationSubmission

interface NotificationGateway {
    fun reserveOrderConfirmation(command: EnsureOrderConfirmationCommand): NotificationSubmission =
        error("Notification submission journal is not configured.")

    fun dispatch(submissionId: Long): NotificationSubmission =
        error("Notification submission journal is not configured.")

    fun ensureOrderConfirmation(command: EnsureOrderConfirmationCommand)
}
