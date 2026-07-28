package com.nexus.shopping.payment.application.port.inbound

import com.nexus.shopping.payment.application.command.FingerprintPaymentAuthorizationCommand

interface FingerprintPaymentAuthorizationInputPort {
    fun fingerprint(command: FingerprintPaymentAuthorizationCommand): String
}
