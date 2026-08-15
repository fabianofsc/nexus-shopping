package com.nexus.shopping.payment.application.port.outbound

import com.nexus.shopping.payment.domain.PaymentAmount
import com.nexus.shopping.payment.domain.PaymentCurrency
import com.nexus.shopping.payment.domain.PaymentProvider
import com.nexus.shopping.payment.domain.PaymentStatus

interface PaymentProviderGateway {
    val provider: PaymentProvider

    fun process(request: ProviderProcessingRequest): ProviderProcessingResult

    fun checkStatus(providerAttemptReference: String): ProviderStatusResult
}

data class ProviderProcessingRequest(
    val referenceId: String,
    val amount: PaymentAmount,
    val currency: PaymentCurrency,
    val paymentToken: String,
    val providerDispatchKey: String,
) {
    override fun toString(): String =
        "ProviderProcessingRequest(referenceId=$referenceId, amount=$amount, currency=$currency, paymentToken=<redacted>, providerDispatchKey=$providerDispatchKey)"
}

data class ProviderProcessingResult(
    val status: PaymentStatus,
    val providerTransactionId: String?,
    val providerAttemptReference: String? = null,
) {
    init {
        if (status == PaymentStatus.REQUESTED) {
            require(providerAttemptReference != null) {
                "A requested (non-terminal) provider processing result must carry a providerAttemptReference."
            }
        }
    }
}

data class ProviderStatusResult(
    val status: PaymentStatus,
    val providerTransactionId: String?,
)
