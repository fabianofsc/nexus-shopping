package com.nexus.shopping.notification.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationMessageRendererTest {
    @Test
    fun `requiredPlaceholders returns orderId and amount for ORDER_CONFIRMED`() {
        val placeholders = NotificationMessageRenderer.requiredPlaceholders(NotificationType.ORDER_CONFIRMED)

        assertEquals(setOf("orderId", "amount"), placeholders)
    }

    @Test
    fun `requiredPlaceholders returns only orderId for ORDER_CANCELLED`() {
        val placeholders = NotificationMessageRenderer.requiredPlaceholders(NotificationType.ORDER_CANCELLED)

        assertEquals(setOf("orderId"), placeholders)
    }

    @Test
    fun `render substitutes placeholders for ORDER_CONFIRMED`() {
        val message =
            NotificationMessageRenderer.render(
                NotificationType.ORDER_CONFIRMED,
                mapOf("orderId" to "123", "amount" to "99.90"),
            )

        assertEquals("Pedido 123 confirmado", message.subject)
        assertEquals("Seu pedido 123 no valor de 99.90 foi confirmado.", message.body)
    }

    @Test
    fun `render keeps placeholder token when param is missing`() {
        val message = NotificationMessageRenderer.render(NotificationType.ORDER_CANCELLED, emptyMap())

        assertEquals("Pedido {orderId} cancelado", message.subject)
    }
}
