package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.integration.checkout.application.model.PaymentValidationData
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentValidationGateway
import com.nexus.shopping.payment.application.command.ValidatePaymentInputCommand
import com.nexus.shopping.payment.application.port.inbound.ValidatePaymentInputPort
import org.springframework.stereotype.Component

@Component
class PaymentValidationGatewayAdapter(
    private val payments: ValidatePaymentInputPort,
) : PaymentValidationGateway {
    override fun validate(data: PaymentValidationData) {
        payments.validate(
            ValidatePaymentInputCommand(
                amount = data.amount,
                currency = data.currency,
            ),
        )
    }
}
