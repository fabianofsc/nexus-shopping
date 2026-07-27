package com.nexus.shopping.payment.domain

import com.nexus.shopping.payment.application.exception.PaymentValidationException
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaymentValueObjectsTest {
    @Test
    fun `normalizes positive amounts to two decimal places`() {
        val amount = PaymentAmount.of(BigDecimal("19.9"))

        assertEquals(BigDecimal("19.90"), amount.value)
    }

    @Test
    fun `rejects zero negative fractional and oversized amounts`() {
        listOf(
            BigDecimal.ZERO,
            BigDecimal("-0.01"),
            BigDecimal("1.999"),
            BigDecimal("10000000000.00"),
        ).forEach { value ->
            assertFailsWith<PaymentValidationException> { PaymentAmount.of(value) }
        }
    }

    @Test
    fun `accepts an uppercase ISO 4217 currency code`() {
        assertEquals("BRL", PaymentCurrency.of("BRL").code)
    }

    @Test
    fun `rejects non ISO or non uppercase currency codes`() {
        listOf("brl", "BR", "ZZZ").forEach { code ->
            assertFailsWith<PaymentValidationException> { PaymentCurrency.of(code) }
        }
    }
}
