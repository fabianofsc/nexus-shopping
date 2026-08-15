package com.nexus.shopping.integration.checkout.application

import com.nexus.shopping.integration.checkout.application.model.ApplyOrderPaymentResultByReferenceCommand
import com.nexus.shopping.integration.checkout.application.model.EnsureOrderConfirmationCommand
import com.nexus.shopping.integration.checkout.application.model.PaymentReconciliationOutcome
import com.nexus.shopping.integration.checkout.application.model.PaymentResultStatus
import com.nexus.shopping.integration.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentReconciliationGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PaymentReconciliationUseCase(
    private val reconciliation: PaymentReconciliationGateway,
    private val orderPaymentResults: OrderPaymentResultGateway,
    private val notifications: NotificationGateway,
) {
    fun reconcile() {
        reconciliation.reconcile().forEach { outcome ->
            try {
                applyOutcome(outcome)
            } catch (exception: RuntimeException) {
                // Caught broadly: this layer cannot reference order/payment-specific exception types
                // (architecture boundary), and one bad outcome must not abort the rest of the batch.
                logger.warn("Failed to apply payment reconciliation outcome for referenceId={}", outcome.referenceId, exception)
            }
        }
    }

    private fun applyOutcome(outcome: PaymentReconciliationOutcome) {
        val applied =
            orderPaymentResults.applyByOrderReference(
                ApplyOrderPaymentResultByReferenceCommand(
                    orderReference = outcome.referenceId,
                    attemptReference = outcome.attemptReference,
                    status = outcome.status.name,
                    providerTransactionId = outcome.providerTransactionId,
                ),
            )
        if (applied.transitioned && outcome.status == PaymentResultStatus.APPROVED) {
            notifications.ensureOrderConfirmation(
                EnsureOrderConfirmationCommand(
                    orderId = applied.orderId,
                    customerId = applied.customerId,
                    recipientEmail = applied.recipientEmail,
                    amount = applied.totalAmount,
                    attemptReference = outcome.attemptReference,
                ),
            )
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(PaymentReconciliationUseCase::class.java)
    }
}
