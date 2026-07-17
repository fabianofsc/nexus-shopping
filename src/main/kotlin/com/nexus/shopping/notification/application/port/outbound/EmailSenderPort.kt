package com.nexus.shopping.notification.application.port.outbound

data class EmailSendResult(
    val success: Boolean,
    val failureReason: String? = null,
)

interface EmailSenderPort {
    fun send(
        to: String,
        subject: String,
        body: String,
    ): EmailSendResult
}
