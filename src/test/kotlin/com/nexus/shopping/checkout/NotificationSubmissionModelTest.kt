package com.nexus.shopping.checkout

import com.nexus.shopping.checkout.application.exception.CheckoutValidationException
import com.nexus.shopping.checkout.application.model.EnsureOrderConfirmationCommand
import com.nexus.shopping.checkout.application.model.NotificationSubmission
import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NotificationSubmissionModelTest {
    @Test
    fun `cria payload estavel para confirmacao do pedido`() {
        val submission = NotificationSubmission.forOrderConfirmation(orderConfirmation())

        assertEquals("order-confirmed:42:attempt-1", submission.notificationKey)
        assertEquals("order:42", submission.referenceId)
        assertEquals("Pedido 42 confirmado", submission.subject)
        assertEquals("Seu pedido 42 no valor de 99.90 foi confirmado.", submission.body)
        assertEquals(NotificationSubmissionStatus.PENDING, submission.status)
    }

    @Test
    fun `descarte e terminal e exige justificativa`() {
        val pending = NotificationSubmission.forOrderConfirmation(orderConfirmation())

        assertFailsWith<CheckoutValidationException> { pending.discard(" ") }
        assertFailsWith<CheckoutValidationException> { pending.discard("a".repeat(501)) }

        val discarded = pending.discard("template remoto invalido")

        assertEquals(NotificationSubmissionStatus.DISCARDED, discarded.status)
        assertEquals("template remoto invalido", discarded.discardReason)
        assertFailsWith<CheckoutValidationException> { discarded.discard("novo motivo") }
        assertFailsWith<CheckoutValidationException> {
            pending.copy(status = NotificationSubmissionStatus.ACCEPTED).discard("novo motivo")
        }
    }

    private fun orderConfirmation() =
        EnsureOrderConfirmationCommand(
            orderId = 42,
            customerId = 9,
            recipientEmail = "cliente@example.com",
            amount = BigDecimal("99.90"),
            attemptReference = "attempt-1",
        )
}
