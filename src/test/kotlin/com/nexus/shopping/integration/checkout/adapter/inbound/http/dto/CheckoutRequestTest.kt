package com.nexus.shopping.integration.checkout.adapter.inbound.http.dto

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckoutRequestTest {
    @Test
    fun `string representation redacts payment token`() {
        val token = "secret-token-that-must-not-leak"
        val request =
            CheckoutRequest(
                customerSnapshot =
                    CustomerSnapshotRequest(
                        name = "Ana Silva",
                        document = "12345678900",
                        documentType = "CPF",
                        email = "ana@example.com",
                        phone = null,
                    ),
                shippingAddressSnapshot =
                    ShippingAddressSnapshotRequest(
                        street = "Rua A",
                        number = "10",
                        complement = null,
                        neighborhood = "Centro",
                        city = "Sao Paulo",
                        state = "SP",
                        zipCode = "01000-000",
                        country = "BR",
                    ),
                paymentToken = token,
            )

        assertFalse(request.toString().contains(token))
        assertTrue(request.toString().contains("paymentToken=<redacted>"))
    }
}
