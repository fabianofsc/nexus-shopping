package com.nexus.shopping.billing

import com.nexus.shopping.billing.application.command.InvoiceCustomerSnapshot
import com.nexus.shopping.billing.application.command.InvoiceItemSnapshot
import com.nexus.shopping.billing.application.command.InvoiceShippingAddressSnapshot
import com.nexus.shopping.billing.application.command.IssueInvoiceCommand
import com.nexus.shopping.billing.application.port.outbound.InvoiceIssuerPort
import com.nexus.shopping.billing.application.usecase.IssueInvoiceUseCase
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class IssueInvoiceUseCaseTest {
    @Test
    fun issuesInvoiceThroughIssuerPort() {
        var received: IssueInvoiceCommand? = null
        val useCase =
            IssueInvoiceUseCase(
                object : InvoiceIssuerPort {
                    override fun issue(command: IssueInvoiceCommand) {
                        received = command
                    }
                },
            )
        val command = invoiceCommand()

        useCase.issue(command)

        assertEquals(command, received)
    }

    private fun invoiceCommand() =
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
            items =
                listOf(
                    InvoiceItemSnapshot(
                        productId = 1L,
                        productName = "Produto A",
                        unitPriceAmount = BigDecimal("19.90"),
                        currency = "BRL",
                        quantity = 2,
                    ),
                ),
            totalAmount = BigDecimal("39.80"),
        )
}
