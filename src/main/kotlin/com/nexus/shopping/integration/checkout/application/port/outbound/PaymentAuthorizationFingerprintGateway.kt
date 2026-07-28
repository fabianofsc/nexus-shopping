package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.PaymentAuthorizationData

interface PaymentAuthorizationFingerprintGateway {
    fun fingerprint(data: PaymentAuthorizationData): String
}
