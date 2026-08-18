package com.nexus.shopping.billing.application.usecase

import com.nexus.shopping.billing.application.command.IssueInvoiceCommand
import com.nexus.shopping.billing.application.port.inbound.IssueInvoiceInputPort
import com.nexus.shopping.billing.application.port.outbound.InvoiceIssuerPort
import org.springframework.stereotype.Service

@Service
class IssueInvoiceUseCase(
    private val issuer: InvoiceIssuerPort,
) : IssueInvoiceInputPort {
    override fun issue(command: IssueInvoiceCommand) = issuer.issue(command)
}
