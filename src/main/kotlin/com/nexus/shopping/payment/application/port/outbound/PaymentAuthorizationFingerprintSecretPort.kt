package com.nexus.shopping.payment.application.port.outbound

interface PaymentAuthorizationFingerprintSecretPort {
    fun secret(): ByteArray
}
