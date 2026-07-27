package com.nexus.shopping.payment.application.port.outbound

import com.nexus.shopping.payment.domain.PaymentAmount
import com.nexus.shopping.payment.domain.PaymentCurrency
import com.nexus.shopping.payment.domain.PaymentStatus

interface PaymentProviderGateway {
    fun process(request: ProviderProcessingRequest): ProviderProcessingResult
}

data class ProviderProcessingRequest(
    val referenceId: String,
    val amount: PaymentAmount,
    val currency: PaymentCurrency,
    val paymentToken: String,
    val providerDispatchKey: String,
)

data class ProviderProcessingResult(
    val status: PaymentStatus,
    val providerTransactionId: String?,
) {
    init {
        require(status != PaymentStatus.REQUESTED) { "Provider processing results must be terminal." }
    }
}
