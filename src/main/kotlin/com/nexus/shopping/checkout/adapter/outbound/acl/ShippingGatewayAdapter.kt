package com.nexus.shopping.checkout.adapter.outbound.acl

import com.nexus.shopping.checkout.application.model.CheckoutShippingCommand
import com.nexus.shopping.checkout.application.port.outbound.ShippingGateway
import com.nexus.shopping.shipping.application.command.ProcessShippingCommand
import com.nexus.shopping.shipping.application.command.ShippingAddressSnapshot
import com.nexus.shopping.shipping.application.command.ShippingCustomerSnapshot
import com.nexus.shopping.shipping.application.command.ShippingItemSnapshot
import com.nexus.shopping.shipping.application.port.inbound.ProcessShippingInputPort
import org.springframework.stereotype.Component

@Component
class ShippingGatewayAdapter(
    private val shipping: ProcessShippingInputPort,
) : ShippingGateway {
    override fun process(command: CheckoutShippingCommand) {
        shipping.process(
            ProcessShippingCommand(
                orderId = command.orderId,
                orderReference = command.orderReference,
                customer = ShippingCustomerSnapshot(name = command.customer.name),
                shippingAddress =
                    ShippingAddressSnapshot(
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
                        ShippingItemSnapshot(
                            productId = it.productId,
                            productName = it.productName,
                            quantity = it.quantity,
                        )
                    },
                totalAmount = command.totalAmount,
            ),
        )
    }
}
