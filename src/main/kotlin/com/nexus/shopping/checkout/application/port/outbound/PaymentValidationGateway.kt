package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.PaymentValidationCommand

interface PaymentValidationGateway {
    fun validate(command: PaymentValidationCommand)
}
