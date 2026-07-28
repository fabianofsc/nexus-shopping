package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.OrderConfirmationNotificationData

interface NotificationGateway {
    fun ensureOrderConfirmation(data: OrderConfirmationNotificationData)
}
