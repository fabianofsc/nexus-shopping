package com.nexus.shopping.notification.application.command

data class SendNotificationCommand(
    val customerId: Long,
    val notificationKey: String,
    val recipientEmail: String,
    val type: String,
    val referenceId: Long?,
    val templateParams: Map<String, String>,
)
