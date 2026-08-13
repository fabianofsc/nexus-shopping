package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.integration.checkout.application.model.PaymentReconciliationOutcome
import com.nexus.shopping.integration.checkout.application.model.PaymentResultStatus
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentReconciliationGateway
import com.nexus.shopping.payment.application.port.inbound.ReconcilePendingPaymentAttemptsInputPort
import org.springframework.stereotype.Component

@Component
class PaymentReconciliationGatewayAdapter(
    private val reconciliation: ReconcilePendingPaymentAttemptsInputPort,
) : PaymentReconciliationGateway {
    override fun reconcile(): List<PaymentReconciliationOutcome> =
        reconciliation.reconcile().map { result ->
            PaymentReconciliationOutcome(
                attemptReference = result.attemptReference,
                referenceId = result.referenceId,
                status = PaymentResultStatus.valueOf(result.status.name),
                providerTransactionId = result.providerTransactionId,
            )
        }
}
