package com.nexus.shopping.billing.adapter.outbound.issuer

import com.nexus.shopping.billing.application.command.IssueInvoiceCommand
import com.nexus.shopping.billing.application.port.outbound.InvoiceIssuerPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class InvoiceIssuerAdapter : InvoiceIssuerPort {
    override fun issue(command: IssueInvoiceCommand) {
        logger.info("billing.invoice.issued order_reference={}", command.orderReference)
        // TODO: substituir o log pela integracao com o emissor fiscal.
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(InvoiceIssuerAdapter::class.java)
    }
}
