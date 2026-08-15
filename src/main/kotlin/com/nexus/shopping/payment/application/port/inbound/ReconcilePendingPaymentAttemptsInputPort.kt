package com.nexus.shopping.payment.application.port.inbound

import com.nexus.shopping.payment.domain.PaymentStatus

interface ReconcilePendingPaymentAttemptsInputPort {
    fun reconcile(): List<PaymentReconciliationResult>
}

data class PaymentReconciliationResult(
    val attemptReference: String,
    val referenceId: String,
    val status: PaymentStatus,
    val providerTransactionId: String?,
)
