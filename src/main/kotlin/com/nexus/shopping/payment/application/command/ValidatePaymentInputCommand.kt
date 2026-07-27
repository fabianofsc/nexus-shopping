package com.nexus.shopping.payment.application.command

import java.math.BigDecimal

data class ValidatePaymentInputCommand(
    val amount: BigDecimal,
    val currency: String,
)
