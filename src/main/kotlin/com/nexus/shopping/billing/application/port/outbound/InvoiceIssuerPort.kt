package com.nexus.shopping.billing.application.port.outbound

import com.nexus.shopping.billing.application.command.IssueInvoiceCommand

interface InvoiceIssuerPort {
    fun issue(command: IssueInvoiceCommand)
}
