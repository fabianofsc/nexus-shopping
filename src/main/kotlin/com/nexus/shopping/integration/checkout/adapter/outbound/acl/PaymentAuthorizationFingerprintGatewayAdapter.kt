package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.integration.checkout.application.model.PaymentAuthorizationCommand
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentAuthorizationFingerprintGateway
import com.nexus.shopping.payment.application.command.FingerprintPaymentAuthorizationCommand
import com.nexus.shopping.payment.application.port.inbound.FingerprintPaymentAuthorizationInputPort
import org.springframework.stereotype.Component

@Component
class PaymentAuthorizationFingerprintGatewayAdapter(
    private val payments: FingerprintPaymentAuthorizationInputPort,
) : PaymentAuthorizationFingerprintGateway {
    override fun fingerprint(command: PaymentAuthorizationCommand): String =
        payments.fingerprint(
            FingerprintPaymentAuthorizationCommand(
                paymentToken = command.paymentToken,
                idempotencyKey = command.idempotencyKey,
            ),
        )
}
