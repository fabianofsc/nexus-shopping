package com.nexus.shopping.checkout.adapter.outbound.acl

import com.nexus.shopping.billing.application.command.InvoiceCustomerSnapshot
import com.nexus.shopping.billing.application.command.InvoiceItemSnapshot
import com.nexus.shopping.billing.application.command.InvoiceShippingAddressSnapshot
import com.nexus.shopping.billing.application.command.IssueInvoiceCommand
import com.nexus.shopping.billing.application.port.inbound.IssueInvoiceInputPort
import com.nexus.shopping.checkout.application.model.CheckoutCustomerSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutInvoiceCommand
import com.nexus.shopping.checkout.application.model.CheckoutItemSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutShippingAddressSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutShippingCommand
import com.nexus.shopping.shipping.application.command.ProcessShippingCommand
import com.nexus.shopping.shipping.application.command.ShippingAddressSnapshot
import com.nexus.shopping.shipping.application.command.ShippingCustomerSnapshot
import com.nexus.shopping.shipping.application.command.ShippingItemSnapshot
import com.nexus.shopping.shipping.application.port.inbound.ProcessShippingInputPort
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class BillingShippingGatewayAdaptersTest {
    @Test
    fun translatesCheckoutInvoiceCommandToBillingCommand() {
        var received: IssueInvoiceCommand? = null
        val gateway =
            BillingGatewayAdapter(
                object : IssueInvoiceInputPort {
                    override fun issue(command: IssueInvoiceCommand) {
                        received = command
                    }
                },
            )

        gateway.issueInvoice(invoiceCommand())

        assertEquals(
            IssueInvoiceCommand(
                orderId = 1L,
                orderReference = "checkout:1",
                customer = InvoiceCustomerSnapshot("Ana Silva", "12345678900", "CPF"),
                shippingAddress = invoiceAddress(),
                items = listOf(InvoiceItemSnapshot(1L, "Produto A", BigDecimal("19.90"), "BRL", 2)),
                totalAmount = BigDecimal("39.80"),
            ),
            received,
        )
    }

    @Test
    fun translatesCheckoutShippingCommandToShippingCommand() {
        var received: ProcessShippingCommand? = null
        val gateway =
            ShippingGatewayAdapter(
                object : ProcessShippingInputPort {
                    override fun process(command: ProcessShippingCommand) {
                        received = command
                    }
                },
            )

        gateway.process(shippingCommand())

        assertEquals(
            ProcessShippingCommand(
                orderId = 1L,
                orderReference = "checkout:1",
                customer = ShippingCustomerSnapshot("Ana Silva"),
                shippingAddress = shippingAddress(),
                items = listOf(ShippingItemSnapshot(1L, "Produto A", 2)),
                totalAmount = BigDecimal("39.80"),
            ),
            received,
        )
    }

    private fun invoiceCommand() =
        CheckoutInvoiceCommand(
            orderId = 1L,
            orderReference = "checkout:1",
            customer = customer(),
            shippingAddress = checkoutAddress(),
            items = listOf(item()),
            totalAmount = BigDecimal("39.80"),
        )

    private fun shippingCommand() =
        CheckoutShippingCommand(
            orderId = 1L,
            orderReference = "checkout:1",
            customer = customer(),
            shippingAddress = checkoutAddress(),
            items = listOf(item()),
            totalAmount = BigDecimal("39.80"),
        )

    private fun customer() = CheckoutCustomerSnapshot(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null)

    private fun checkoutAddress() = CheckoutShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR")

    private fun invoiceAddress() = InvoiceShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR")

    private fun shippingAddress() = ShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR")

    private fun item() = CheckoutItemSnapshot(1L, "Produto A", BigDecimal("19.90"), "BRL", 2)
}
