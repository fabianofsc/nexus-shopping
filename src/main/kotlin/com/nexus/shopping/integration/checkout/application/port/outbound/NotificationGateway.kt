package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.EnsureOrderConfirmationCommand

interface NotificationGateway {
    fun ensureOrderConfirmation(command: EnsureOrderConfirmationCommand)
}
