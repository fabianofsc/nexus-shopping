package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.PaymentValidationCommand

interface PaymentValidationGateway {
    fun validate(command: PaymentValidationCommand)
}
