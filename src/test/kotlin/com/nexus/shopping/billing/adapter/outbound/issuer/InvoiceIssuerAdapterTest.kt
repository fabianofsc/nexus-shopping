package com.nexus.shopping.billing.adapter.outbound.issuer

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.nexus.shopping.billing.application.command.InvoiceCustomerSnapshot
import com.nexus.shopping.billing.application.command.InvoiceItemSnapshot
import com.nexus.shopping.billing.application.command.InvoiceShippingAddressSnapshot
import com.nexus.shopping.billing.application.command.IssueInvoiceCommand
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class InvoiceIssuerAdapterTest {
    @Test
    fun recordsInvoiceIssuedWithOrderReferenceOnly() {
        val logger = LoggerFactory.getLogger(InvoiceIssuerAdapter::class.java) as Logger
        val events = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(events)

        try {
            InvoiceIssuerAdapter().issue(command())

            assertEquals(1, events.list.size)
            assertEquals(
                "billing.invoice.issued order_reference=checkout:1",
                events.list.single().formattedMessage,
            )
            assertFalse(
                events.list
                    .single()
                    .formattedMessage
                    .contains("12345678900"),
            )
            assertFalse(
                events.list
                    .single()
                    .formattedMessage
                    .contains("Rua A"),
            )
        } finally {
            logger.detachAppender(events)
            events.stop()
        }
    }

    private fun command() =
        IssueInvoiceCommand(
            orderId = 1L,
            orderReference = "checkout:1",
            customer = InvoiceCustomerSnapshot("Ana Silva", "12345678900", "CPF"),
            shippingAddress =
                InvoiceShippingAddressSnapshot(
                    street = "Rua A",
                    number = "10",
                    complement = null,
                    neighborhood = "Centro",
                    city = "Sao Paulo",
                    state = "SP",
                    zipCode = "01000-000",
                    country = "BR",
                ),
            items = listOf(InvoiceItemSnapshot(1L, "Produto A", BigDecimal("19.90"), "BRL", 2)),
            totalAmount = BigDecimal("39.80"),
        )
}
