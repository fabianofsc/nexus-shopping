package com.nexus.shopping.notification.adapter.outbound.email

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoggingEmailSenderAdapterTest {
    @Test
    fun `send always reports success without failureReason`() {
        val adapter = LoggingEmailSenderAdapter()

        val result = adapter.send("cliente@example.com", "Pedido 123 confirmado", "Seu pedido foi confirmado.")

        assertTrue(result.success)
        assertNull(result.failureReason)
        assertEquals(true, result.success)
    }
}
