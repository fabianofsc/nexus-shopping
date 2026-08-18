package com.nexus.shopping.checkout.adapter.inbound.http.dto

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckoutRequestTest {
    @Test
    fun `string representation redacts payment token`() {
        val token = "secret-token-that-must-not-leak"
        val request = CheckoutRequest(paymentToken = token)

        assertFalse(request.toString().contains(token))
        assertTrue(request.toString().contains("paymentToken=<redacted>"))
    }
}
