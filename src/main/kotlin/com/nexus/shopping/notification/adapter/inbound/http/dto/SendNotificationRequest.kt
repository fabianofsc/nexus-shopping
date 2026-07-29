package com.nexus.shopping.notification.adapter.inbound.http.dto

import com.nexus.shopping.notification.application.command.SendNotificationCommand

data class SendNotificationRequest(
    val customerId: Long,
    val notificationKey: String,
    val recipientEmail: String,
    val type: String,
    val referenceId: Long? = null,
    val templateParams: Map<String, String> = emptyMap(),
)

fun SendNotificationRequest.toCommand(): SendNotificationCommand =
    SendNotificationCommand(
        customerId = customerId,
        notificationKey = notificationKey,
        recipientEmail = recipientEmail,
        type = type,
        referenceId = referenceId,
        templateParams = templateParams,
    )
