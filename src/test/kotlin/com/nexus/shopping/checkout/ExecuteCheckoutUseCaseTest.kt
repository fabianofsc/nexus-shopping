package com.nexus.shopping.checkout

import com.nexus.shopping.checkout.application.model.AppliedOrderPaymentResult
import com.nexus.shopping.checkout.application.model.ApplyOrderPaymentResultByReferenceCommand
import com.nexus.shopping.checkout.application.model.ApplyOrderPaymentResultCommand
import com.nexus.shopping.checkout.application.model.CheckoutCartSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutCommand
import com.nexus.shopping.checkout.application.model.CheckoutCustomerResolution
import com.nexus.shopping.checkout.application.model.CheckoutCustomerSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutItemSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutOrderSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutShippingAddressSnapshot
import com.nexus.shopping.checkout.application.model.CreateCheckoutOrderCommand
import com.nexus.shopping.checkout.application.model.EnsureOrderConfirmationCommand
import com.nexus.shopping.checkout.application.model.FindCheckoutOrderReplayCommand
import com.nexus.shopping.checkout.application.model.PaymentAuthorizationCommand
import com.nexus.shopping.checkout.application.model.PaymentProcessingCommand
import com.nexus.shopping.checkout.application.model.PaymentProcessingResult
import com.nexus.shopping.checkout.application.model.PaymentResultStatus
import com.nexus.shopping.checkout.application.model.PaymentValidationCommand
import com.nexus.shopping.checkout.application.port.outbound.CheckoutCartGateway
import com.nexus.shopping.checkout.application.port.outbound.CheckoutCustomerGateway
import com.nexus.shopping.checkout.application.port.outbound.InventoryGateway
import com.nexus.shopping.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.checkout.application.port.outbound.OrderCreationGateway
import com.nexus.shopping.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentAuthorizationFingerprintGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentProcessingGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentValidationGateway
import com.nexus.shopping.checkout.application.port.outbound.TransactionPort
import com.nexus.shopping.checkout.application.usecase.ExecuteCheckoutUseCase
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame

class ExecuteCheckoutUseCaseTest {
    @Test
    fun `coordinates reserve create and confirm inside one transaction`() {
        val events = mutableListOf<String>()
        val carts = RecordingCartGateway(events)
        val orders = RecordingOrderGateway(events)
        val transactions =
            object : TransactionPort {
                override fun <T> inTransaction(block: () -> T): T {
                    events += "transaction:start"
                    return block().also { events += "transaction:end" }
                }
            }

        val result = workflow(carts, orders, transactions, events).execute(command())

        assertEquals(
            listOf(
                "fingerprint",
                "customer",
                "transaction:start",
                "replay",
                "reserve",
                "replay",
                "validate",
                "create",
                "decrement",
                "confirm",
                "transaction:end",
                "payment",
            ),
            events,
        )
        assertEquals(false, result.replayed)
        assertEquals("checkout:1", result.orderReference)
        assertEquals("ana@example.com", result.recipientEmail)
        assertEquals(100L, orders.createdCommand?.cartId)
        assertEquals(listOf(item()), orders.createdCommand?.items)
    }

    @Test
    fun `returns replay before touching Cart`() {
        val events = mutableListOf<String>()
        val replay = order(replayed = true)
        val carts = RecordingCartGateway(events)
        val orders = RecordingOrderGateway(events, replay = replay)

        val result = workflow(carts, orders, ImmediateTransaction, events).execute(command())

        assertEquals(replay, result)
        assertEquals(listOf("fingerprint", "customer", "replay", "payment"), events)
    }

    @Test
    fun `propagates replay returned by create without confirming Cart`() {
        val events = mutableListOf<String>()
        val replay = order(replayed = true)
        val carts = RecordingCartGateway(events)
        val orders = RecordingOrderGateway(events, createdOrder = replay)

        val result = workflow(carts, orders, ImmediateTransaction, events).execute(command())

        assertEquals(replay, result)
        assertEquals(listOf("fingerprint", "customer", "replay", "reserve", "replay", "validate", "create", "payment"), events)
    }

    @Test
    fun `rolls back transaction when order creation fails after reservation`() {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("order creation failed")
        val carts = RecordingCartGateway(events)
        val orders = RecordingOrderGateway(events, creationFailure = failure)
        val transactions =
            object : TransactionPort {
                override fun <T> inTransaction(block: () -> T): T {
                    events += "transaction:start"
                    return try {
                        block().also { events += "transaction:end" }
                    } catch (exception: RuntimeException) {
                        events += "transaction:rollback"
                        throw exception
                    }
                }
            }

        val thrown =
            assertFailsWith<IllegalStateException> {
                workflow(carts, orders, transactions, events).execute(command())
            }

        assertSame(failure, thrown)
        assertEquals(
            listOf(
                "fingerprint",
                "customer",
                "transaction:start",
                "replay",
                "reserve",
                "replay",
                "validate",
                "create",
                "transaction:rollback",
            ),
            events,
        )
    }

    @Test
    fun `does not notify when payment is rejected and leaves the stock release to reconciliation`() {
        val events = mutableListOf<String>()
        val rejectedOrder = order(replayed = false).copy(status = "PAYMENT_FAILED", awaitingPayment = false)
        val checkout =
            ExecuteCheckoutUseCase(
                carts = RecordingCartGateway(events),
                customers = RecordingCustomerGateway(events),
                orders = RecordingOrderGateway(events),
                paymentAuthorizationFingerprints =
                    object : PaymentAuthorizationFingerprintGateway {
                        override fun fingerprint(command: PaymentAuthorizationCommand): String {
                            events += "fingerprint"
                            return "opaque-payment-authorization-fingerprint"
                        }
                    },
                paymentValidation =
                    object : PaymentValidationGateway {
                        override fun validate(command: PaymentValidationCommand) {
                            events += "validate"
                        }
                    },
                payments =
                    object : PaymentProcessingGateway {
                        override fun process(command: PaymentProcessingCommand): PaymentProcessingResult {
                            events += "payment"
                            return PaymentProcessingResult("pay-rejected", PaymentResultStatus.REJECTED, null, replayed = false)
                        }
                    },
                orderPaymentResults =
                    object : OrderPaymentResultGateway {
                        override fun apply(command: ApplyOrderPaymentResultCommand): CheckoutOrderSnapshot {
                            events += "apply"
                            return rejectedOrder
                        }

                        override fun applyByOrderReference(command: ApplyOrderPaymentResultByReferenceCommand): AppliedOrderPaymentResult =
                            error("Not used by the checkout workflow")
                    },
                notifications =
                    object : NotificationGateway {
                        override fun ensureOrderConfirmation(command: EnsureOrderConfirmationCommand) {
                            events += "notify"
                        }
                    },
                inventory = RecordingInventoryGateway(events),
                transaction = ImmediateTransaction,
            ).execute(command())

        assertEquals(
            listOf(
                "fingerprint",
                "customer",
                "replay",
                "reserve",
                "replay",
                "validate",
                "create",
                "decrement",
                "confirm",
                "payment",
                "apply",
            ),
            events,
        )
        assertFalse(events.contains("notify"))
        assertEquals("PAYMENT_FAILED", rejectedOrder.status)
    }

    private fun command() =
        CheckoutCommand(
            customerId = 10L,
            paymentToken = "approved",
            idempotencyKey = "checkout-1",
        )

    private fun workflow(
        carts: CheckoutCartGateway,
        orders: OrderCreationGateway,
        transactions: TransactionPort,
        events: MutableList<String>,
        inventory: InventoryGateway = RecordingInventoryGateway(events),
        customers: CheckoutCustomerGateway = RecordingCustomerGateway(events),
    ) = ExecuteCheckoutUseCase(
        carts = carts,
        customers = customers,
        orders = orders,
        paymentAuthorizationFingerprints =
            object : PaymentAuthorizationFingerprintGateway {
                override fun fingerprint(command: PaymentAuthorizationCommand): String {
                    events += "fingerprint"
                    return "opaque-payment-authorization-fingerprint"
                }
            },
        paymentValidation =
            object : PaymentValidationGateway {
                override fun validate(command: PaymentValidationCommand) {
                    events += "validate"
                }
            },
        payments =
            object : PaymentProcessingGateway {
                override fun process(command: PaymentProcessingCommand): PaymentProcessingResult {
                    events += "payment"
                    return PaymentProcessingResult("pay-requested", PaymentResultStatus.REQUESTED, null, replayed = false)
                }
            },
        orderPaymentResults =
            object : OrderPaymentResultGateway {
                override fun apply(command: ApplyOrderPaymentResultCommand): CheckoutOrderSnapshot = error("Not used for REQUESTED")

                override fun applyByOrderReference(command: ApplyOrderPaymentResultByReferenceCommand): AppliedOrderPaymentResult =
                    error("Not used for REQUESTED")
            },
        notifications =
            object : NotificationGateway {
                override fun ensureOrderConfirmation(command: EnsureOrderConfirmationCommand) = error("Not used for REQUESTED")
            },
        inventory = inventory,
        transaction = transactions,
    )

    private fun item() = CheckoutItemSnapshot(1L, "Produto A", BigDecimal("19.90"), "BRL", 2)

    private fun customer() = CheckoutCustomerSnapshot(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null)

    private fun shippingAddress() = CheckoutShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR")

    private fun resolution() = CheckoutCustomerResolution(customer(), shippingAddress())

    private fun order(replayed: Boolean) =
        CheckoutOrderSnapshot(
            id = 1L,
            orderReference = "checkout:1",
            customerId = 10L,
            cartId = 100L,
            recipientEmail = "ana@example.com",
            customerSnapshot = customer(),
            shippingAddressSnapshot = shippingAddress(),
            items = listOf(item()),
            totalAmount = BigDecimal("39.80"),
            status = "WAITING_PAYMENT",
            awaitingPayment = true,
            createdAt = Instant.parse("2026-07-26T12:00:00Z"),
            cancelledAt = null,
            replayed = replayed,
        )

    private inner class RecordingCartGateway(
        private val events: MutableList<String>,
    ) : CheckoutCartGateway {
        override fun reserveActiveCart(customerId: Long): CheckoutCartSnapshot {
            events += "reserve"
            return CheckoutCartSnapshot(100L, customerId, listOf(item()))
        }

        override fun confirmCheckout(reservationId: Long) {
            events += "confirm"
        }
    }

    private inner class RecordingOrderGateway(
        private val events: MutableList<String>,
        private val replay: CheckoutOrderSnapshot? = null,
        private val createdOrder: CheckoutOrderSnapshot = order(replayed = false),
        private val creationFailure: RuntimeException? = null,
    ) : OrderCreationGateway {
        var createdCommand: CreateCheckoutOrderCommand? = null

        override fun findReplay(command: FindCheckoutOrderReplayCommand): CheckoutOrderSnapshot? {
            events += "replay"
            return replay
        }

        override fun create(command: CreateCheckoutOrderCommand): CheckoutOrderSnapshot {
            events += "create"
            createdCommand = command
            creationFailure?.let { throw it }
            return createdOrder
        }
    }

    private class RecordingInventoryGateway(
        private val events: MutableList<String>,
    ) : InventoryGateway {
        override fun decrement(
            orderReference: String,
            items: List<CheckoutItemSnapshot>,
        ) {
            events += "decrement"
        }

        override fun release(
            orderReference: String,
            items: List<CheckoutItemSnapshot>,
        ) {
            events += "release"
        }
    }

    private inner class RecordingCustomerGateway(
        private val events: MutableList<String>,
    ) : CheckoutCustomerGateway {
        override fun resolve(customerId: Long): CheckoutCustomerResolution {
            events += "customer"
            return resolution()
        }
    }

    private object ImmediateTransaction : TransactionPort {
        override fun <T> inTransaction(block: () -> T): T = block()
    }
}
