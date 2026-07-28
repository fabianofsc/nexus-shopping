package com.nexus.shopping.payment.adapter.outbound.security

import com.nexus.shopping.payment.application.port.outbound.PaymentAuthorizationFingerprintSecretPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ConfiguredPaymentAuthorizationFingerprintSecret(
    @param:Value("\${nexus.payment.authorization-fingerprint-secret:development-only-change-me}")
    private val configuredSecret: String,
) : PaymentAuthorizationFingerprintSecretPort {
    override fun secret(): ByteArray = configuredSecret.toByteArray(Charsets.UTF_8)
}
