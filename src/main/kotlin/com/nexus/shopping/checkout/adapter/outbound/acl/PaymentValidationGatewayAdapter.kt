package com.nexus.shopping.checkout.adapter.outbound.acl

import com.nexus.shopping.checkout.application.model.PaymentValidationCommand
import com.nexus.shopping.checkout.application.port.outbound.PaymentValidationGateway
import com.nexus.shopping.payment.application.command.ValidatePaymentInputCommand
import com.nexus.shopping.payment.application.port.inbound.ValidatePaymentInputPort
import org.springframework.stereotype.Component

@Component
class PaymentValidationGatewayAdapter(
    private val payments: ValidatePaymentInputPort,
) : PaymentValidationGateway {
    override fun validate(command: PaymentValidationCommand) {
        payments.validate(
            ValidatePaymentInputCommand(
                amount = command.amount,
                currency = command.currency,
            ),
        )
    }
}
