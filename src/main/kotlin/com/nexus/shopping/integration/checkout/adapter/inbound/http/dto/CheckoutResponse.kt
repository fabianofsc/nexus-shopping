package com.nexus.shopping.integration.checkout.adapter.inbound.http.dto

import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderSnapshot
import java.math.BigDecimal
import java.time.Instant

data class CheckoutResponse(
    val id: Long,
    val customerId: Long,
    val cartId: Long,
    val customerSnapshot: CustomerSnapshotResponse,
    val shippingAddressSnapshot: ShippingAddressSnapshotResponse,
    val items: List<CheckoutItemResponse>,
    val totalAmount: BigDecimal,
    val status: String,
    val createdAt: Instant,
    val cancelledAt: Instant?,
)

data class CustomerSnapshotResponse(
    val customerId: Long,
    val name: String,
    val document: String,
    val documentType: String,
    val email: String,
    val phone: String?,
)

data class ShippingAddressSnapshotResponse(
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
)

data class CheckoutItemResponse(
    val productId: Long,
    val productName: String,
    val unitPriceAmount: BigDecimal,
    val currency: String,
    val quantity: Int,
    val totalAmount: BigDecimal,
)

fun CheckoutOrderSnapshot.toResponse() =
    CheckoutResponse(
        id,
        customerId,
        cartId,
        CustomerSnapshotResponse(
            customerSnapshot.customerId,
            customerSnapshot.name,
            customerSnapshot.document,
            customerSnapshot.documentType,
            customerSnapshot.email,
            customerSnapshot.phone,
        ),
        ShippingAddressSnapshotResponse(
            shippingAddressSnapshot.street,
            shippingAddressSnapshot.number,
            shippingAddressSnapshot.complement,
            shippingAddressSnapshot.neighborhood,
            shippingAddressSnapshot.city,
            shippingAddressSnapshot.state,
            shippingAddressSnapshot.zipCode,
            shippingAddressSnapshot.country,
        ),
        items.map { item ->
            CheckoutItemResponse(
                item.productId,
                item.productName,
                item.unitPriceAmount,
                item.currency,
                item.quantity,
                item.totalAmount,
            )
        },
        totalAmount,
        status,
        createdAt,
        cancelledAt,
    )
