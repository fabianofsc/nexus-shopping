package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.PaymentValidationData

interface PaymentValidationGateway {
    fun validate(data: PaymentValidationData)
}
