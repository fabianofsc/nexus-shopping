package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.PaymentAuthorizationCommand

interface PaymentAuthorizationFingerprintGateway {
    fun fingerprint(command: PaymentAuthorizationCommand): String
}
