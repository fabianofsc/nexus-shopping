package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.PaymentProcessingData
import com.nexus.shopping.integration.checkout.application.model.PaymentResultData

interface PaymentProcessingGateway {
    fun process(data: PaymentProcessingData): PaymentResultData
}
