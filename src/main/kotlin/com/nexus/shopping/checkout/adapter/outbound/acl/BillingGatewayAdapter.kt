package com.nexus.shopping.checkout.adapter.outbound.acl

import com.nexus.shopping.billing.application.command.InvoiceCustomerSnapshot
import com.nexus.shopping.billing.application.command.InvoiceItemSnapshot
import com.nexus.shopping.billing.application.command.InvoiceShippingAddressSnapshot
import com.nexus.shopping.billing.application.command.IssueInvoiceCommand
import com.nexus.shopping.billing.application.port.inbound.IssueInvoiceInputPort
import com.nexus.shopping.checkout.application.model.CheckoutInvoiceCommand
import com.nexus.shopping.checkout.application.port.outbound.BillingGateway
import org.springframework.stereotype.Component

@Component
class BillingGatewayAdapter(
    private val billing: IssueInvoiceInputPort,
) : BillingGateway {
    override fun issueInvoice(command: CheckoutInvoiceCommand) {
        billing.issue(
            IssueInvoiceCommand(
                orderId = command.orderId,
                orderReference = command.orderReference,
                customer =
                    InvoiceCustomerSnapshot(
                        name = command.customer.name,
                        document = command.customer.document,
                        documentType = command.customer.documentType,
                    ),
                shippingAddress =
                    InvoiceShippingAddressSnapshot(
                        street = command.shippingAddress.street,
                        number = command.shippingAddress.number,
                        complement = command.shippingAddress.complement,
                        neighborhood = command.shippingAddress.neighborhood,
                        city = command.shippingAddress.city,
                        state = command.shippingAddress.state,
                        zipCode = command.shippingAddress.zipCode,
                        country = command.shippingAddress.country,
                    ),
                items =
                    command.items.map {
                        InvoiceItemSnapshot(
                            productId = it.productId,
                            productName = it.productName,
                            unitPriceAmount = it.unitPriceAmount,
                            currency = it.currency,
                            quantity = it.quantity,
                        )
                    },
                totalAmount = command.totalAmount,
            ),
        )
    }
}
