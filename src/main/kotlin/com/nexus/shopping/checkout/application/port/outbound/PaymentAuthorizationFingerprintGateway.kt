package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.PaymentAuthorizationCommand

interface PaymentAuthorizationFingerprintGateway {
    fun fingerprint(command: PaymentAuthorizationCommand): String
}
