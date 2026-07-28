package com.nexus.shopping.integration.checkout.adapter.outbound.local

import com.nexus.shopping.integration.checkout.application.model.ApplyOrderPaymentResultData
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.order.application.command.ApplyOrderPaymentResultCommand
import com.nexus.shopping.order.application.port.inbound.ApplyOrderPaymentResultInputPort
import org.springframework.stereotype.Component

@Component
class LocalOrderPaymentResultGateway(
    private val orders: ApplyOrderPaymentResultInputPort,
) : OrderPaymentResultGateway {
    override fun apply(data: ApplyOrderPaymentResultData): CheckoutOrderData {
        val updated =
            orders.apply(
                ApplyOrderPaymentResultCommand(
                    orderId = data.order.id,
                    attemptReference = data.payment.attemptReference,
                    status = data.payment.status.name,
                    providerTransactionId = data.payment.providerTransactionId,
                ),
            )
        return updated.toCheckoutData(replayed = data.order.replayed)
    }
}
