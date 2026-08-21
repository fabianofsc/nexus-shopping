package com.nexus.shopping.checkout.application.usecase

import com.nexus.shopping.checkout.application.model.ApplyOrderPaymentResultByReferenceCommand
import com.nexus.shopping.checkout.application.model.CheckoutInvoiceCommand
import com.nexus.shopping.checkout.application.model.CheckoutShippingCommand
import com.nexus.shopping.checkout.application.model.EnsureOrderConfirmationCommand
import com.nexus.shopping.checkout.application.model.PaymentReconciliationOutcome
import com.nexus.shopping.checkout.application.model.PaymentResultStatus
import com.nexus.shopping.checkout.application.port.inbound.ReconcilePaymentsInputPort
import com.nexus.shopping.checkout.application.port.outbound.BillingGateway
import com.nexus.shopping.checkout.application.port.outbound.InventoryGateway
import com.nexus.shopping.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentReconciliationGateway
import com.nexus.shopping.checkout.application.port.outbound.ShippingGateway
import com.nexus.shopping.checkout.application.port.outbound.TransactionPort
import org.slf4j.LoggerFactory

class PaymentReconciliationUseCase(
    private val reconciliation: PaymentReconciliationGateway,
    private val orderPaymentResults: OrderPaymentResultGateway,
    private val billing: BillingGateway,
    private val shipping: ShippingGateway,
    private val notifications: NotificationGateway,
    private val inventory: InventoryGateway,
    private val transaction: TransactionPort,
) : ReconcilePaymentsInputPort {
    override fun reconcile() {
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
        val command =
            ApplyOrderPaymentResultByReferenceCommand(
                orderReference = outcome.referenceId,
                attemptReference = outcome.attemptReference,
                status = outcome.status.name,
                providerTransactionId = outcome.providerTransactionId,
            )
        if (outcome.status == PaymentResultStatus.APPROVED) {
            val appliedAndSubmission =
                transaction.inTransaction {
                    val applied = orderPaymentResults.applyByOrderReference(command)
                    if (!applied.transitioned) return@inTransaction applied to null
                    val submission =
                        notifications.reserveOrderConfirmation(
                            EnsureOrderConfirmationCommand(
                                orderId = applied.orderId,
                                customerId = applied.customerId,
                                recipientEmail = applied.recipientEmail,
                                amount = applied.totalAmount,
                                attemptReference = outcome.attemptReference,
                            ),
                        )
                    applied to submission
                }
            val (applied, submission) = appliedAndSubmission
            if (submission != null) {
                billing.issueInvoice(CheckoutInvoiceCommand.from(applied.order))
                shipping.process(CheckoutShippingCommand.from(applied.order))
                notifications.dispatch(requireNotNull(submission.id) { "reserved notification submission must have an id." })
            }
            return
        }

        transaction.inTransaction {
            val applied = orderPaymentResults.applyByOrderReference(command)
            if (applied.transitioned && outcome.status == PaymentResultStatus.REJECTED) {
                // The stock was reserved when the order was created; the provider only rejects it later,
                // so this is the single place that gives it back. Guarded by `transitioned` because
                // ReleaseStockUseCase increments unconditionally and this runs on every polling cycle.
                inventory.release(outcome.referenceId, applied.items)
            }
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(PaymentReconciliationUseCase::class.java)
    }
}
