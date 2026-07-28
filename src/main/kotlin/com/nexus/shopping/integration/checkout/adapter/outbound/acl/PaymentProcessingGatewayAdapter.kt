package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.integration.checkout.application.model.PaymentProcessingCommand
import com.nexus.shopping.integration.checkout.application.model.PaymentProcessingResult
import com.nexus.shopping.integration.checkout.application.model.PaymentResultStatus
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentProcessingGateway
import com.nexus.shopping.payment.application.command.ProcessPaymentCommand
import com.nexus.shopping.payment.application.port.inbound.ProcessPaymentInputPort
import org.springframework.stereotype.Component

@Component
class PaymentProcessingGatewayAdapter(
    private val payments: ProcessPaymentInputPort,
) : PaymentProcessingGateway {
    override fun process(command: PaymentProcessingCommand): PaymentProcessingResult {
        val result =
            payments.process(
                ProcessPaymentCommand(
                    referenceId = command.referenceId,
                    amount = command.amount,
                    currency = command.currency,
                    paymentToken = command.paymentToken,
                    idempotencyKey = command.idempotencyKey,
                ),
            )
        return PaymentProcessingResult(
            attemptReference = result.attemptReference,
            status = PaymentResultStatus.valueOf(result.status.name),
            providerTransactionId = result.providerTransactionId,
            replayed = result.replayed,
        )
    }
}
