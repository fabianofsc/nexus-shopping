package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.PaymentProcessingCommand
import com.nexus.shopping.checkout.application.model.PaymentProcessingResult

interface PaymentProcessingGateway {
    fun process(command: PaymentProcessingCommand): PaymentProcessingResult
}
