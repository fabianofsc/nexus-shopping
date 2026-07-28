package com.nexus.shopping.integration.checkout.application

import com.nexus.shopping.integration.checkout.application.exception.CheckoutValidationException
import com.nexus.shopping.integration.checkout.application.model.ApplyOrderPaymentResultData
import com.nexus.shopping.integration.checkout.application.model.CheckoutCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutItemData
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData
import com.nexus.shopping.integration.checkout.application.model.CreateCheckoutOrderCommand
import com.nexus.shopping.integration.checkout.application.model.FindCheckoutOrderReplayCommand
import com.nexus.shopping.integration.checkout.application.model.OrderConfirmationNotificationData
import com.nexus.shopping.integration.checkout.application.model.PaymentAuthorizationData
import com.nexus.shopping.integration.checkout.application.model.PaymentProcessingData
import com.nexus.shopping.integration.checkout.application.model.PaymentResultStatus
import com.nexus.shopping.integration.checkout.application.model.PaymentValidationData
import com.nexus.shopping.integration.checkout.application.port.outbound.CheckoutCartGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderCreationGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentAuthorizationFingerprintGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentProcessingGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentValidationGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.TransactionPort
import org.springframework.stereotype.Service

@Service
class CheckoutWorkflowUseCase(
    private val carts: CheckoutCartGateway,
    private val orders: OrderCreationGateway,
    private val paymentAuthorizationFingerprints: PaymentAuthorizationFingerprintGateway,
    private val paymentValidation: PaymentValidationGateway,
    private val payments: PaymentProcessingGateway,
    private val orderPaymentResults: OrderPaymentResultGateway,
    private val notifications: NotificationGateway,
    private val transaction: TransactionPort,
) {
    fun execute(command: CheckoutCommand): CheckoutOrderData {
        val paymentAuthorizationFingerprint =
            paymentAuthorizationFingerprints.fingerprint(
                PaymentAuthorizationData(
                    paymentToken = command.paymentToken,
                    idempotencyKey = command.idempotencyKey,
                ),
            )
        val replayData =
            FindCheckoutOrderReplayCommand(
                customerId = command.customerId,
                customerSnapshot = command.customerSnapshot,
                shippingAddressSnapshot = command.shippingAddressSnapshot,
                idempotencyKey = command.idempotencyKey,
                paymentAuthorizationFingerprint = paymentAuthorizationFingerprint,
            )
        val order =
            transaction.inTransaction {
                orders.findReplay(replayData)?.let { return@inTransaction it }

                val cart =
                    try {
                        carts.reserveActiveCart(command.customerId)
                    } catch (exception: CheckoutValidationException) {
                        orders.findReplay(replayData)?.let { return@inTransaction it }
                        throw exception
                    }
                orders.findReplay(replayData)?.let { return@inTransaction it }
                if (cart.items.isEmpty()) throw CheckoutValidationException("cart items must not be empty.")
                paymentValidation.validate(
                    PaymentValidationData(
                        amount = cart.totalAmount,
                        currency = cart.items.singleCurrency(),
                    ),
                )

                val createdOrder =
                    orders.create(
                        CreateCheckoutOrderCommand(
                            customerId = command.customerId,
                            cartId = cart.reservationId,
                            customerSnapshot = command.customerSnapshot,
                            shippingAddressSnapshot = command.shippingAddressSnapshot,
                            items = cart.items,
                            idempotencyKey = command.idempotencyKey,
                            paymentAuthorizationFingerprint = paymentAuthorizationFingerprint,
                        ),
                    )
                if (!createdOrder.replayed && createdOrder.cartId == cart.reservationId) {
                    carts.confirmCheckout(cart.reservationId)
                }
                createdOrder
            }

        val payment =
            payments.process(
                PaymentProcessingData(
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
                ApplyOrderPaymentResultData(
                    order = order,
                    payment = payment,
                ),
            )
        if (payment.status == PaymentResultStatus.APPROVED) {
            notifications.ensureOrderConfirmation(
                OrderConfirmationNotificationData(
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

    private fun List<CheckoutItemData>.singleCurrency(): String {
        val currencies = map { it.currency }.distinct()
        if (currencies.size != 1) throw CheckoutValidationException("cart items must use a single currency.")
        return currencies.single()
    }
}
