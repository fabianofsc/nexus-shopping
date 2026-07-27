package com.nexus.shopping.payment.domain

import com.nexus.shopping.payment.application.exception.PaymentValidationException
import java.math.BigDecimal
import java.math.RoundingMode

@ConsistentCopyVisibility
data class PaymentAmount private constructor(
    val value: BigDecimal,
) {
    companion object {
        private val maximum = BigDecimal("9999999999.99")

        fun of(value: BigDecimal): PaymentAmount {
            if (value.signum() <= 0) invalid("amount must be greater than zero.")
            val normalized =
                try {
                    value.setScale(2, RoundingMode.UNNECESSARY)
                } catch (_: ArithmeticException) {
                    invalid("amount must have at most two decimal places.")
                }
            if (normalized > maximum) invalid("amount exceeds NUMERIC(12, 2).")
            return PaymentAmount(normalized)
        }

        private fun invalid(message: String): Nothing = throw PaymentValidationException(message)
    }
}
