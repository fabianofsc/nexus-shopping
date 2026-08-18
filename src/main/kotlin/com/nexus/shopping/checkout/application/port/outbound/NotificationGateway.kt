package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.EnsureOrderConfirmationCommand

interface NotificationGateway {
    fun ensureOrderConfirmation(command: EnsureOrderConfirmationCommand)
}
