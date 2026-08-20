package com.nexus.shopping.billing.application.port.inbound

import com.nexus.shopping.billing.application.command.IssueInvoiceCommand

interface IssueInvoiceInputPort {
    fun issue(command: IssueInvoiceCommand)
}
