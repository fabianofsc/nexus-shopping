package com.nexus.shopping.notification.adapter.outbound.email

import com.nexus.shopping.notification.application.port.outbound.EmailSendResult
import com.nexus.shopping.notification.application.port.outbound.EmailSenderPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LoggingEmailSenderAdapter : EmailSenderPort {
    override fun send(
        to: String,
        subject: String,
        body: String,
    ): EmailSendResult {
        logger.info("Simulated email sent to {} subject={}", to, subject)
        return EmailSendResult(success = true)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(LoggingEmailSenderAdapter::class.java)
    }
}
