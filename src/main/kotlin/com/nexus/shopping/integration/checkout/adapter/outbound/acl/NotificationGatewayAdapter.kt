package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.integration.checkout.application.model.EnsureOrderConfirmationCommand
import com.nexus.shopping.integration.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.notification.application.command.SendNotificationCommand
import com.nexus.shopping.notification.application.port.inbound.SendNotificationInputPort
import org.springframework.stereotype.Component

@Component
class NotificationGatewayAdapter(
    private val notifications: SendNotificationInputPort,
) : NotificationGateway {
    override fun ensureOrderConfirmation(command: EnsureOrderConfirmationCommand) {
        notifications.send(
            SendNotificationCommand(
                customerId = command.customerId,
                notificationKey = "order-confirmed:${command.orderId}:${command.attemptReference}",
                recipientEmail = command.recipientEmail,
                type = "ORDER_CONFIRMED",
                referenceId = command.orderId,
                templateParams =
                    mapOf(
                        "orderId" to command.orderId.toString(),
                        "amount" to command.amount.toPlainString(),
                    ),
            ),
        )
    }
}
