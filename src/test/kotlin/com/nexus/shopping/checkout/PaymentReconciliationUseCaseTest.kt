package com.nexus.shopping.checkout

import com.nexus.shopping.checkout.application.model.AppliedOrderPaymentResult
import com.nexus.shopping.checkout.application.model.ApplyOrderPaymentResultByReferenceCommand
import com.nexus.shopping.checkout.application.model.ApplyOrderPaymentResultCommand
import com.nexus.shopping.checkout.application.model.CheckoutCustomerSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutInvoiceCommand
import com.nexus.shopping.checkout.application.model.CheckoutItemSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutOrderSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutShippingAddressSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutShippingCommand
import com.nexus.shopping.checkout.application.model.EnsureOrderConfirmationCommand
import com.nexus.shopping.checkout.application.model.NotificationSubmission
import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import com.nexus.shopping.checkout.application.model.PaymentReconciliationOutcome
import com.nexus.shopping.checkout.application.model.PaymentResultStatus
import com.nexus.shopping.checkout.application.port.outbound.BillingGateway
import com.nexus.shopping.checkout.application.port.outbound.InventoryGateway
import com.nexus.shopping.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentReconciliationGateway
import com.nexus.shopping.checkout.application.port.outbound.ShippingGateway
import com.nexus.shopping.checkout.application.port.outbound.TransactionPort
import com.nexus.shopping.checkout.application.usecase.PaymentReconciliationUseCase
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaymentReconciliationUseCaseTest {
    @Test
    fun `payment reconciliation receives billing and shipping gateways`() {
        val gatewayTypes =
            PaymentReconciliationUseCase::class.java.constructors
                .single()
                .parameterTypes
                .toSet()

        assertTrue(BillingGateway::class.java in gatewayTypes)
        assertTrue(ShippingGateway::class.java in gatewayTypes)
    }

    @Test
    fun `an approved outcome reserves in the transaction then invoices ships and dispatches`() {
        val notifications = mutableListOf<EnsureOrderConfirmationCommand>()
        val invoices = mutableListOf<CheckoutInvoiceCommand>()
        val shipments = mutableListOf<CheckoutShippingCommand>()
        val events = mutableListOf<String>()
        val useCase =
            useCase(
                outcomes = listOf(outcome(referenceId = "checkout:1", status = PaymentResultStatus.APPROVED)),
                applyResults = mapOf("checkout:1" to appliedResult(orderId = 1L, status = "CONFIRMED", transitioned = true)),
                notifications = notifications,
                invoices = invoices,
                shipments = shipments,
                events = events,
            )

        useCase.reconcile()

        assertEquals(
            listOf(
                "transaction:start",
                "apply",
                "notification:reserve",
                "transaction:commit",
                "invoice",
                "shipping",
                "notification:dispatch",
            ),
            events,
        )
        assertEquals("checkout:1", invoices.single().orderReference)
        assertEquals("checkout:1", shipments.single().orderReference)
        assertEquals(1, notifications.size)
        assertEquals(1L, notifications.single().orderId)
    }

    @Test
    fun `a rejected outcome releases the reserved stock without sending a notification`() {
        val notifications = mutableListOf<EnsureOrderConfirmationCommand>()
        val releases = mutableListOf<Pair<String, List<CheckoutItemSnapshot>>>()
        val useCase =
            useCase(
                outcomes = listOf(outcome(referenceId = "checkout:2", status = PaymentResultStatus.REJECTED)),
                applyResults = mapOf("checkout:2" to appliedResult(orderId = 2L, status = "PAYMENT_FAILED", transitioned = true)),
                notifications = notifications,
                releases = releases,
            )

        useCase.reconcile()

        assertEquals(0, notifications.size)
        assertEquals(1, releases.size)
        assertEquals("checkout:2", releases.single().first)
        assertEquals(listOf(10L), releases.single().second.map { it.productId })
    }

    @Test
    fun `a rejected outcome that did not actually transition the order does not release stock twice`() {
        val releases = mutableListOf<Pair<String, List<CheckoutItemSnapshot>>>()
        val useCase =
            useCase(
                outcomes = listOf(outcome(referenceId = "checkout:5", status = PaymentResultStatus.REJECTED)),
                applyResults = mapOf("checkout:5" to appliedResult(orderId = 5L, status = "PAYMENT_FAILED", transitioned = false)),
                notifications = mutableListOf(),
                releases = releases,
            )

        useCase.reconcile()

        assertEquals(0, releases.size)
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

    @Test
    fun `dispatch failure for a reconciled order records a failed submission`() {
        val reserved = mutableListOf<NotificationSubmission>()
        val dispatched = mutableListOf<NotificationSubmission>()
        val useCase =
            useCase(
                outcomes = listOf(outcome(referenceId = "checkout:6", status = PaymentResultStatus.APPROVED)),
                applyResults = mapOf("checkout:6" to appliedResult(orderId = 6L, status = "CONFIRMED", transitioned = true)),
                notifications = mutableListOf(),
                reserved = reserved,
                dispatched = dispatched,
                dispatchStatus = NotificationSubmissionStatus.FAILED,
            )

        useCase.reconcile()

        assertEquals(NotificationSubmissionStatus.FAILED, dispatched.single().status)
    }

    @Test
    fun `billing failure keeps its submission pending without dispatch and does not stop later outcomes`() {
        val reserved = mutableListOf<NotificationSubmission>()
        val dispatched = mutableListOf<NotificationSubmission>()
        val releases = mutableListOf<Pair<String, List<CheckoutItemSnapshot>>>()
        val useCase =
            useCase(
                outcomes =
                    listOf(
                        outcome(referenceId = "checkout:7", status = PaymentResultStatus.APPROVED),
                        outcome(referenceId = "checkout:8", status = PaymentResultStatus.REJECTED),
                    ),
                applyResults =
                    mapOf(
                        "checkout:7" to appliedResult(orderId = 7L, status = "CONFIRMED", transitioned = true),
                        "checkout:8" to appliedResult(orderId = 8L, status = "PAYMENT_FAILED", transitioned = true),
                    ),
                notifications = mutableListOf(),
                reserved = reserved,
                dispatched = dispatched,
                releases = releases,
                billingFailure = IllegalStateException("issuer unavailable"),
            )

        useCase.reconcile()

        assertEquals(NotificationSubmissionStatus.PENDING, reserved.single().status)
        assertEquals(emptyList(), dispatched)
        assertEquals(listOf("checkout:8"), releases.map { it.first })
    }

    @Test
    fun `shipping failure keeps its submission pending without dispatch and does not stop later outcomes`() {
        val reserved = mutableListOf<NotificationSubmission>()
        val dispatched = mutableListOf<NotificationSubmission>()
        val releases = mutableListOf<Pair<String, List<CheckoutItemSnapshot>>>()
        val useCase =
            useCase(
                outcomes =
                    listOf(
                        outcome(referenceId = "checkout:9", status = PaymentResultStatus.APPROVED),
                        outcome(referenceId = "checkout:10", status = PaymentResultStatus.REJECTED),
                    ),
                applyResults =
                    mapOf(
                        "checkout:9" to appliedResult(orderId = 9L, status = "CONFIRMED", transitioned = true),
                        "checkout:10" to appliedResult(orderId = 10L, status = "PAYMENT_FAILED", transitioned = true),
                    ),
                notifications = mutableListOf(),
                reserved = reserved,
                dispatched = dispatched,
                releases = releases,
                shippingFailure = IllegalStateException("carrier unavailable"),
            )

        useCase.reconcile()

        assertEquals(NotificationSubmissionStatus.PENDING, reserved.single().status)
        assertEquals(emptyList(), dispatched)
        assertEquals(listOf("checkout:10"), releases.map { it.first })
    }

    private fun useCase(
        outcomes: List<PaymentReconciliationOutcome>,
        applyResults: Map<String, AppliedOrderPaymentResult>,
        notifications: MutableList<EnsureOrderConfirmationCommand>,
        releases: MutableList<Pair<String, List<CheckoutItemSnapshot>>> = mutableListOf(),
        invoices: MutableList<CheckoutInvoiceCommand> = mutableListOf(),
        shipments: MutableList<CheckoutShippingCommand> = mutableListOf(),
        events: MutableList<String> = mutableListOf(),
        reserved: MutableList<NotificationSubmission> = mutableListOf(),
        dispatched: MutableList<NotificationSubmission> = mutableListOf(),
        dispatchStatus: NotificationSubmissionStatus = NotificationSubmissionStatus.ACCEPTED,
        billingFailure: RuntimeException? = null,
        shippingFailure: RuntimeException? = null,
    ) = PaymentReconciliationUseCase(
        reconciliation = FakePaymentReconciliationGateway(outcomes),
        orderPaymentResults = FakeOrderPaymentResultGateway(applyResults, events),
        billing = RecordingBillingGateway(invoices, events, billingFailure),
        shipping = RecordingShippingGateway(shipments, events, shippingFailure),
        notifications = RecordingNotificationGateway(notifications, reserved, dispatched, events, dispatchStatus),
        inventory = RecordingInventoryGateway(releases),
        transaction =
            object : TransactionPort {
                override fun <T> inTransaction(block: () -> T): T {
                    events += "transaction:start"
                    return block().also { events += "transaction:commit" }
                }
            },
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
        order =
            CheckoutOrderSnapshot(
                id = orderId,
                orderReference = "checkout:$orderId",
                customerId = 10L,
                cartId = 20L,
                recipientEmail = "customer-$orderId@example.com",
                customerSnapshot = CheckoutCustomerSnapshot(10L, "Customer", "12345678901", "CPF", "customer-$orderId@example.com", null),
                shippingAddressSnapshot =
                    CheckoutShippingAddressSnapshot("Street", "10", null, "Center", "Sao Paulo", "SP", "01000-000", "BR"),
                items = listOf(CheckoutItemSnapshot(10L, "Product 10", BigDecimal("19.90"), "BRL", 2)),
                totalAmount = BigDecimal("39.80"),
                status = status,
                awaitingPayment = false,
                createdAt = Instant.EPOCH,
                cancelledAt = null,
                replayed = false,
            ),
        transitioned = transitioned,
    )

    private class FakePaymentReconciliationGateway(
        private val outcomes: List<PaymentReconciliationOutcome>,
    ) : PaymentReconciliationGateway {
        override fun reconcile(): List<PaymentReconciliationOutcome> = outcomes
    }

    private class FakeOrderPaymentResultGateway(
        private val applyResults: Map<String, AppliedOrderPaymentResult>,
        private val events: MutableList<String>,
    ) : OrderPaymentResultGateway {
        override fun apply(command: ApplyOrderPaymentResultCommand): CheckoutOrderSnapshot = error("Not used by this fake.")

        override fun applyByOrderReference(command: ApplyOrderPaymentResultByReferenceCommand): AppliedOrderPaymentResult {
            events += "apply"
            return applyResults[command.orderReference] ?: throw NoSuchElementException("No order for reference ${command.orderReference}")
        }
    }

    private class RecordingInventoryGateway(
        private val releases: MutableList<Pair<String, List<CheckoutItemSnapshot>>>,
    ) : InventoryGateway {
        override fun decrement(
            orderReference: String,
            items: List<CheckoutItemSnapshot>,
        ) = error("Reconciliation never decrements stock.")

        override fun release(
            orderReference: String,
            items: List<CheckoutItemSnapshot>,
        ) {
            releases += orderReference to items
        }
    }

    private class RecordingNotificationGateway(
        private val notifications: MutableList<EnsureOrderConfirmationCommand>,
        private val reserved: MutableList<NotificationSubmission>,
        private val dispatched: MutableList<NotificationSubmission>,
        private val events: MutableList<String>,
        private val dispatchStatus: NotificationSubmissionStatus,
    ) : NotificationGateway {
        override fun ensureOrderConfirmation(command: EnsureOrderConfirmationCommand) {
            notifications += command
            events += "notification"
        }

        override fun reserveOrderConfirmation(command: EnsureOrderConfirmationCommand): NotificationSubmission {
            notifications += command
            events += "notification:reserve"
            return NotificationSubmission.forOrderConfirmation(command).copy(id = 1L).also(reserved::add)
        }

        override fun dispatch(submissionId: Long): NotificationSubmission {
            events += "notification:dispatch"
            return requireNotNull(reserved.singleOrNull { it.id == submissionId })
                .copy(status = dispatchStatus)
                .also(dispatched::add)
        }
    }

    private class RecordingBillingGateway(
        private val invoices: MutableList<CheckoutInvoiceCommand>,
        private val events: MutableList<String>,
        private val failure: RuntimeException?,
    ) : BillingGateway {
        override fun issueInvoice(command: CheckoutInvoiceCommand) {
            invoices += command
            events += "invoice"
            failure?.let { throw it }
        }
    }

    private class RecordingShippingGateway(
        private val shipments: MutableList<CheckoutShippingCommand>,
        private val events: MutableList<String>,
        private val failure: RuntimeException?,
    ) : ShippingGateway {
        override fun process(command: CheckoutShippingCommand) {
            shipments += command
            events += "shipping"
            failure?.let { throw it }
        }
    }
}
