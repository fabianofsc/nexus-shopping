package com.nexus.shopping.integration.checkout.adapter.outbound.local

import com.nexus.shopping.integration.checkout.application.model.PaymentProcessingData
import com.nexus.shopping.integration.checkout.application.model.PaymentResultData
import com.nexus.shopping.integration.checkout.application.model.PaymentResultStatus
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentProcessingGateway
import com.nexus.shopping.payment.application.command.ProcessPaymentCommand
import com.nexus.shopping.payment.application.port.inbound.ProcessPaymentInputPort
import org.springframework.stereotype.Component

@Component
class LocalPaymentProcessingGateway(
    private val payments: ProcessPaymentInputPort,
) : PaymentProcessingGateway {
    override fun process(data: PaymentProcessingData): PaymentResultData {
        val result =
            payments.process(
                ProcessPaymentCommand(
                    referenceId = data.referenceId,
                    amount = data.amount,
                    currency = data.currency,
                    paymentToken = data.paymentToken,
                    idempotencyKey = data.idempotencyKey,
                ),
            )
        return PaymentResultData(
            attemptReference = result.attemptReference,
            status = PaymentResultStatus.valueOf(result.status.name),
            providerTransactionId = result.providerTransactionId,
            replayed = result.replayed,
        )
    }
}
