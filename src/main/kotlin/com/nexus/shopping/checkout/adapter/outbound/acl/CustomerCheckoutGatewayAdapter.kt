package com.nexus.shopping.checkout.adapter.outbound.acl

import com.nexus.shopping.checkout.application.exception.CheckoutValidationException
import com.nexus.shopping.checkout.application.model.CheckoutCustomerResolution
import com.nexus.shopping.checkout.application.model.CheckoutCustomerSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutShippingAddressSnapshot
import com.nexus.shopping.checkout.application.port.outbound.CheckoutCustomerGateway
import com.nexus.shopping.customer.application.port.inbound.GetCustomerSnapshotInputPort
import org.springframework.stereotype.Component

@Component
class CustomerCheckoutGatewayAdapter(
    private val customerSnapshot: GetCustomerSnapshotInputPort,
) : CheckoutCustomerGateway {
    override fun resolve(customerId: Long): CheckoutCustomerResolution {
        val customer =
            customerSnapshot.getSnapshot(customerId)
                ?: throw CheckoutValidationException("customerId $customerId does not reference an existing customer.")
        val contact = customer.contact
        val address = customer.address
        return CheckoutCustomerResolution(
            customer =
                CheckoutCustomerSnapshot(
                    customer.id,
                    customer.name,
                    customer.document,
                    customer.documentType.name,
                    contact.email,
                    contact.phone,
                ),
            shippingAddress =
                CheckoutShippingAddressSnapshot(
                    address.street,
                    address.number,
                    address.complement,
                    address.neighborhood,
                    address.city,
                    address.state,
                    address.zipCode,
                    address.country,
                ),
        )
    }
}
