package com.nexus.shopping.payment.application.port.inbound

import com.nexus.shopping.payment.application.command.ProcessPaymentCommand
import com.nexus.shopping.payment.domain.PaymentStatus

interface ProcessPaymentInputPort {
    fun process(command: ProcessPaymentCommand): PaymentProcessingResult
}

data class PaymentProcessingResult(
    val attemptReference: String,
    val referenceId: String,
    val status: PaymentStatus,
    val providerTransactionId: String?,
    val replayed: Boolean,
)
