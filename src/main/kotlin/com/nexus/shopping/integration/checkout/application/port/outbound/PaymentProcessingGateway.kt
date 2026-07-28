package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.PaymentProcessingCommand
import com.nexus.shopping.integration.checkout.application.model.PaymentProcessingResult

interface PaymentProcessingGateway {
    fun process(command: PaymentProcessingCommand): PaymentProcessingResult
}
