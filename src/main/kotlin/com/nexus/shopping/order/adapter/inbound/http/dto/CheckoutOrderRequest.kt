package com.nexus.shopping.order.adapter.inbound.http.dto

import com.nexus.shopping.order.application.command.CheckoutOrderCommand
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.ShippingAddressSnapshot

data class CheckoutOrderRequest(
    val customerSnapshot: CustomerSnapshotRequest,
    val shippingAddressSnapshot: ShippingAddressSnapshotRequest,
)

data class CustomerSnapshotRequest(
    val name: String,
    val document: String,
    val documentType: String,
    val email: String,
    val phone: String?,
)

data class ShippingAddressSnapshotRequest(
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
)

fun CheckoutOrderRequest.toCommand(
    customerId: Long,
    idempotencyKey: String,
): CheckoutOrderCommand =
    CheckoutOrderCommand(
        customerId = customerId,
        customerSnapshot =
            CustomerSnapshot(
                customerId = customerId,
                name = customerSnapshot.name,
                document = customerSnapshot.document,
                documentType = customerSnapshot.documentType,
                email = customerSnapshot.email,
                phone = customerSnapshot.phone,
            ),
        shippingAddressSnapshot =
            ShippingAddressSnapshot(
                street = shippingAddressSnapshot.street,
                number = shippingAddressSnapshot.number,
                complement = shippingAddressSnapshot.complement,
                neighborhood = shippingAddressSnapshot.neighborhood,
                city = shippingAddressSnapshot.city,
                state = shippingAddressSnapshot.state,
                zipCode = shippingAddressSnapshot.zipCode,
                country = shippingAddressSnapshot.country,
            ),
        idempotencyKey = idempotencyKey,
    )
