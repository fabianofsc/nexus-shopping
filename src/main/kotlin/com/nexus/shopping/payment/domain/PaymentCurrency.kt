package com.nexus.shopping.payment.domain

import java.util.Currency

@ConsistentCopyVisibility
data class PaymentCurrency private constructor(
    val code: String,
) {
    companion object {
        private val isoCodes = Currency.getAvailableCurrencies().map { it.currencyCode }.toSet()

        fun of(code: String): PaymentCurrency {
            if (code.length != 3 || code != code.uppercase() || code !in isoCodes) {
                throw PaymentDomainValidationException("currency must be an uppercase ISO 4217 code.")
            }
            return PaymentCurrency(code)
        }
    }
}
