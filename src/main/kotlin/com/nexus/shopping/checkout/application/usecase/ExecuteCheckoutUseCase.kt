package com.nexus.shopping.checkout.application.usecase

import com.nexus.shopping.checkout.application.exception.CheckoutValidationException
import com.nexus.shopping.checkout.application.model.ApplyOrderPaymentResultCommand
import com.nexus.shopping.checkout.application.model.CheckoutCommand
import com.nexus.shopping.checkout.application.model.CheckoutInvoiceCommand
import com.nexus.shopping.checkout.application.model.CheckoutItemSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutOrderSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutShippingCommand
import com.nexus.shopping.checkout.application.model.CreateCheckoutOrderCommand
import com.nexus.shopping.checkout.application.model.EnsureOrderConfirmationCommand
import com.nexus.shopping.checkout.application.model.FindCheckoutOrderReplayCommand
import com.nexus.shopping.checkout.application.model.PaymentAuthorizationCommand
import com.nexus.shopping.checkout.application.model.PaymentProcessingCommand
import com.nexus.shopping.checkout.application.model.PaymentResultStatus
import com.nexus.shopping.checkout.application.model.PaymentValidationCommand
import com.nexus.shopping.checkout.application.port.inbound.ExecuteCheckoutInputPort
import com.nexus.shopping.checkout.application.port.outbound.CheckoutCartGateway
import com.nexus.shopping.checkout.application.port.outbound.CheckoutCustomerGateway
import com.nexus.shopping.checkout.application.port.outbound.BillingGateway
import com.nexus.shopping.checkout.application.port.outbound.InventoryGateway
import com.nexus.shopping.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.checkout.application.port.outbound.OrderCreationGateway
import com.nexus.shopping.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentAuthorizationFingerprintGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentProcessingGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentValidationGateway
import com.nexus.shopping.checkout.application.port.outbound.ShippingGateway
import com.nexus.shopping.checkout.application.port.outbound.TransactionPort

class ExecuteCheckoutUseCase(
    private val carts: CheckoutCartGateway,
    private val customers: CheckoutCustomerGateway,
    private val orders: OrderCreationGateway,
    private val paymentAuthorizationFingerprints: PaymentAuthorizationFingerprintGateway,
    private val paymentValidation: PaymentValidationGateway,
    private val payments: PaymentProcessingGateway,
    private val orderPaymentResults: OrderPaymentResultGateway,
    private val billing: BillingGateway,
    private val shipping: ShippingGateway,
    private val notifications: NotificationGateway,
    private val inventory: InventoryGateway,
    private val transaction: TransactionPort,
) : ExecuteCheckoutInputPort {
    override fun execute(command: CheckoutCommand): CheckoutOrderSnapshot {
        val paymentAuthorizationFingerprint =
            paymentAuthorizationFingerprints.fingerprint(
                PaymentAuthorizationCommand(
                    paymentToken = command.paymentToken,
                    idempotencyKey = command.idempotencyKey,
                ),
            )
        val resolution = customers.resolve(command.customerId)
        val replayCommand =
            FindCheckoutOrderReplayCommand(
                customerId = command.customerId,
                customerSnapshot = resolution.customer,
                shippingAddressSnapshot = resolution.shippingAddress,
                idempotencyKey = command.idempotencyKey,
                paymentAuthorizationFingerprint = paymentAuthorizationFingerprint,
            )
        val order =
            transaction.inTransaction {
                orders.findReplay(replayCommand)?.let { return@inTransaction it }

                val cart =
                    try {
                        carts.reserveActiveCart(command.customerId)
                    } catch (exception: CheckoutValidationException) {
                        orders.findReplay(replayCommand)?.let { return@inTransaction it }
                        throw exception
                    }
                orders.findReplay(replayCommand)?.let { return@inTransaction it }
                if (cart.items.isEmpty()) throw CheckoutValidationException("cart items must not be empty.")
                paymentValidation.validate(
                    PaymentValidationCommand(
                        amount = cart.totalAmount,
                        currency = cart.items.singleCurrency(),
                    ),
                )

                val createdOrder =
                    orders.create(
                        CreateCheckoutOrderCommand(
                            customerId = command.customerId,
                            cartId = cart.reservationId,
                            customerSnapshot = resolution.customer,
                            shippingAddressSnapshot = resolution.shippingAddress,
                            items = cart.items,
                            idempotencyKey = command.idempotencyKey,
                            paymentAuthorizationFingerprint = paymentAuthorizationFingerprint,
                        ),
                    )
                if (!createdOrder.replayed && createdOrder.cartId == cart.reservationId) {
                    inventory.decrement(createdOrder.orderReference, cart.items)
                    carts.confirmCheckout(cart.reservationId)
                }
                createdOrder
            }

        val payment =
            payments.process(
                PaymentProcessingCommand(
                    referenceId = order.orderReference,
                    amount = order.totalAmount,
                    currency = order.items.singleCurrency(),
                    paymentToken = command.paymentToken,
                    idempotencyKey = command.idempotencyKey,
                ),
            )
        if (payment.status == PaymentResultStatus.REQUESTED) return order

        val updatedOrder =
            orderPaymentResults.apply(
                ApplyOrderPaymentResultCommand(
                    order = order,
                    payment = payment,
                ),
            )
        if (payment.status == PaymentResultStatus.REJECTED) {
            inventory.release(order.orderReference, order.items)
        }
        if (payment.status == PaymentResultStatus.APPROVED) {
            billing.issueInvoice(CheckoutInvoiceCommand.from(updatedOrder))
            shipping.process(CheckoutShippingCommand.from(updatedOrder))
            notifications.ensureOrderConfirmation(
                EnsureOrderConfirmationCommand(
                    orderId = updatedOrder.id,
                    customerId = updatedOrder.customerId,
                    recipientEmail = updatedOrder.recipientEmail,
                    amount = updatedOrder.totalAmount,
                    attemptReference = payment.attemptReference,
                ),
            )
        }
        return updatedOrder
    }

    private fun List<CheckoutItemSnapshot>.singleCurrency(): String {
        val currencies = map { it.currency }.distinct()
        if (currencies.size != 1) throw CheckoutValidationException("cart items must use a single currency.")
        return currencies.single()
    }
}
