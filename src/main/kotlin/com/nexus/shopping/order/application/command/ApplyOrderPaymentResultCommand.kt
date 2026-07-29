package com.nexus.shopping.order.application.command

data class ApplyOrderPaymentResultCommand(
    val orderId: Long,
    val attemptReference: String,
    val status: String,
    val providerTransactionId: String?,
)
