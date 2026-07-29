package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.integration.checkout.application.model.ApplyOrderPaymentResultCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderSnapshot
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.order.application.port.inbound.ApplyOrderPaymentResultInputPort
import org.springframework.stereotype.Component
import com.nexus.shopping.order.application.command.ApplyOrderPaymentResultCommand as OrderApplyOrderPaymentResultCommand

@Component
class OrderPaymentResultGatewayAdapter(
    private val orders: ApplyOrderPaymentResultInputPort,
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
}
