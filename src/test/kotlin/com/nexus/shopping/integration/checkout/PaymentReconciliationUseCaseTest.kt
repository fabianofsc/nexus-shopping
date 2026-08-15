package com.nexus.shopping.integration.checkout

import com.nexus.shopping.integration.checkout.application.PaymentReconciliationUseCase
import com.nexus.shopping.integration.checkout.application.model.AppliedOrderPaymentResult
import com.nexus.shopping.integration.checkout.application.model.ApplyOrderPaymentResultByReferenceCommand
import com.nexus.shopping.integration.checkout.application.model.ApplyOrderPaymentResultCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderSnapshot
import com.nexus.shopping.integration.checkout.application.model.EnsureOrderConfirmationCommand
import com.nexus.shopping.integration.checkout.application.model.PaymentReconciliationOutcome
import com.nexus.shopping.integration.checkout.application.model.PaymentResultStatus
import com.nexus.shopping.integration.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentReconciliationGateway
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentReconciliationUseCaseTest {
    @Test
    fun `an approved outcome confirms the order and sends exactly one notification`() {
        val notifications = mutableListOf<EnsureOrderConfirmationCommand>()
        val useCase =
            useCase(
                outcomes = listOf(outcome(referenceId = "checkout:1", status = PaymentResultStatus.APPROVED)),
                applyResults = mapOf("checkout:1" to appliedResult(orderId = 1L, status = "CONFIRMED", transitioned = true)),
                notifications = notifications,
            )

        useCase.reconcile()

        assertEquals(1, notifications.size)
        assertEquals(1L, notifications.single().orderId)
    }

    @Test
    fun `a rejected outcome marks the order payment failed without sending a notification`() {
        val notifications = mutableListOf<EnsureOrderConfirmationCommand>()
        val useCase =
            useCase(
                outcomes = listOf(outcome(referenceId = "checkout:2", status = PaymentResultStatus.REJECTED)),
                applyResults = mapOf("checkout:2" to appliedResult(orderId = 2L, status = "PAYMENT_FAILED", transitioned = true)),
                notifications = notifications,
            )

        useCase.reconcile()

        assertEquals(0, notifications.size)
    }

    @Test
    fun `a reference id without a matching order does not interrupt the rest of the batch`() {
        val notifications = mutableListOf<EnsureOrderConfirmationCommand>()
        val useCase =
            useCase(
                outcomes =
                    listOf(
                        outcome(referenceId = "checkout:missing", status = PaymentResultStatus.APPROVED),
                        outcome(referenceId = "checkout:3", status = PaymentResultStatus.APPROVED),
                    ),
                applyResults = mapOf("checkout:3" to appliedResult(orderId = 3L, status = "CONFIRMED", transitioned = true)),
                notifications = notifications,
            )

        useCase.reconcile()

        assertEquals(1, notifications.size)
        assertEquals(3L, notifications.single().orderId)
    }

    @Test
    fun `an approved outcome that did not actually transition the order does not send a notification`() {
        val notifications = mutableListOf<EnsureOrderConfirmationCommand>()
        val useCase =
            useCase(
                outcomes = listOf(outcome(referenceId = "checkout:4", status = PaymentResultStatus.APPROVED)),
                applyResults = mapOf("checkout:4" to appliedResult(orderId = 4L, status = "CONFIRMED", transitioned = false)),
                notifications = notifications,
            )

        useCase.reconcile()

        assertEquals(0, notifications.size)
    }

    private fun useCase(
        outcomes: List<PaymentReconciliationOutcome>,
        applyResults: Map<String, AppliedOrderPaymentResult>,
        notifications: MutableList<EnsureOrderConfirmationCommand>,
    ) = PaymentReconciliationUseCase(
        reconciliation = FakePaymentReconciliationGateway(outcomes),
        orderPaymentResults = FakeOrderPaymentResultGateway(applyResults),
        notifications = RecordingNotificationGateway(notifications),
    )

    private fun outcome(
        referenceId: String,
        status: PaymentResultStatus,
        attemptReference: String = "pay_$referenceId",
    ) = PaymentReconciliationOutcome(
        attemptReference = attemptReference,
        referenceId = referenceId,
        status = status,
        providerTransactionId = if (status == PaymentResultStatus.APPROVED) "provider-tx" else null,
    )

    private fun appliedResult(
        orderId: Long,
        status: String,
        transitioned: Boolean,
    ) = AppliedOrderPaymentResult(
        orderId = orderId,
        customerId = 10L,
        recipientEmail = "customer-$orderId@example.com",
        totalAmount = BigDecimal("39.80"),
        status = status,
        transitioned = transitioned,
    )

    private class FakePaymentReconciliationGateway(
        private val outcomes: List<PaymentReconciliationOutcome>,
    ) : PaymentReconciliationGateway {
        override fun reconcile(): List<PaymentReconciliationOutcome> = outcomes
    }

    private class FakeOrderPaymentResultGateway(
        private val applyResults: Map<String, AppliedOrderPaymentResult>,
    ) : OrderPaymentResultGateway {
        override fun apply(command: ApplyOrderPaymentResultCommand): CheckoutOrderSnapshot = error("Not used by this fake.")

        override fun applyByOrderReference(command: ApplyOrderPaymentResultByReferenceCommand): AppliedOrderPaymentResult =
            applyResults[command.orderReference] ?: throw NoSuchElementException("No order for reference ${command.orderReference}")
    }

    private class RecordingNotificationGateway(
        private val notifications: MutableList<EnsureOrderConfirmationCommand>,
    ) : NotificationGateway {
        override fun ensureOrderConfirmation(command: EnsureOrderConfirmationCommand) {
            notifications += command
        }
    }
}
