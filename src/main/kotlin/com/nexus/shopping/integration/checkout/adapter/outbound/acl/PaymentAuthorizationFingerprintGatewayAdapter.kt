package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.integration.checkout.application.model.PaymentAuthorizationData
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentAuthorizationFingerprintGateway
import com.nexus.shopping.payment.application.command.FingerprintPaymentAuthorizationCommand
import com.nexus.shopping.payment.application.port.inbound.FingerprintPaymentAuthorizationInputPort
import org.springframework.stereotype.Component

@Component
class PaymentAuthorizationFingerprintGatewayAdapter(
    private val payments: FingerprintPaymentAuthorizationInputPort,
) : PaymentAuthorizationFingerprintGateway {
    override fun fingerprint(data: PaymentAuthorizationData): String =
        payments.fingerprint(
            FingerprintPaymentAuthorizationCommand(
                paymentToken = data.paymentToken,
                idempotencyKey = data.idempotencyKey,
            ),
        )
}
