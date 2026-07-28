package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.integration.checkout.application.model.OrderConfirmationNotificationData
import com.nexus.shopping.integration.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.notification.application.command.SendNotificationCommand
import com.nexus.shopping.notification.application.port.inbound.SendNotificationInputPort
import org.springframework.stereotype.Component

@Component
class NotificationGatewayAdapter(
    private val notifications: SendNotificationInputPort,
) : NotificationGateway {
    override fun ensureOrderConfirmation(data: OrderConfirmationNotificationData) {
        notifications.send(
            SendNotificationCommand(
                customerId = data.customerId,
                notificationKey = "order-confirmed:${data.orderId}:${data.attemptReference}",
                recipientEmail = data.recipientEmail,
                type = "ORDER_CONFIRMED",
                referenceId = data.orderId,
                templateParams =
                    mapOf(
                        "orderId" to data.orderId.toString(),
                        "amount" to data.amount.toPlainString(),
                    ),
            ),
        )
    }
}
