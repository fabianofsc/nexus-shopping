package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.CheckoutInvoiceCommand

interface BillingGateway {
    fun issueInvoice(command: CheckoutInvoiceCommand)
}
