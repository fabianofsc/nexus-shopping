package com.nexus.shopping.checkout.adapter.outbound.acl

import com.nexus.shopping.checkout.application.model.AppliedOrderPaymentResult
import com.nexus.shopping.checkout.application.model.ApplyOrderPaymentResultByReferenceCommand
import com.nexus.shopping.checkout.application.model.ApplyOrderPaymentResultCommand
import com.nexus.shopping.checkout.application.model.CheckoutOrderSnapshot
import com.nexus.shopping.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.order.application.exception.OrderNotFoundException
import com.nexus.shopping.order.application.port.inbound.ApplyOrderPaymentResultInputPort
import com.nexus.shopping.order.application.port.inbound.GetOrderByIdInputPort
import com.nexus.shopping.order.domain.OrderStatus
import org.springframework.stereotype.Component
import com.nexus.shopping.order.application.command.ApplyOrderPaymentResultCommand as OrderApplyOrderPaymentResultCommand

@Component
class OrderPaymentResultGatewayAdapter(
    private val orders: ApplyOrderPaymentResultInputPort,
    private val getOrderById: GetOrderByIdInputPort,
) : OrderPaymentResultGateway {
    override fun apply(command: ApplyOrderPaymentResultCommand): CheckoutOrderSnapshot {
        val updated =
            orders.apply(
                OrderApplyOrderPaymentResultCommand(
                    orderId = command.order.id,
                    attemptReference = command.payment.attemptReference,
                    status = command.payment.status.name,
                    providerTransactionId = command.payment.providerTransactionId,
                ),
            )
        return updated.toCheckoutSnapshot(replayed = command.order.replayed)
    }

    override fun applyByOrderReference(command: ApplyOrderPaymentResultByReferenceCommand): AppliedOrderPaymentResult {
        val orderId =
            parseCheckoutOrderReference(command.orderReference)
                ?: throw OrderNotFoundException("Order reference ${command.orderReference} is not a valid checkout order reference.")
        val before = getOrderById.execute(orderId)
        val transitioned = before.status == OrderStatus.WAITING_PAYMENT && before.paymentAttemptReference != command.attemptReference

        val updated =
            orders.apply(
                OrderApplyOrderPaymentResultCommand(
                    orderId = orderId,
                    attemptReference = command.attemptReference,
                    status = command.status,
                    providerTransactionId = command.providerTransactionId,
                ),
            )
        return AppliedOrderPaymentResult(
            orderId = requireNotNull(updated.id),
            customerId = updated.customerId,
            recipientEmail = updated.customerSnapshot.email,
            items = updated.toCheckoutSnapshot(replayed = false).items,
            totalAmount = updated.totalAmount,
            status = updated.status.name,
            transitioned = transitioned,
        )
    }
}
