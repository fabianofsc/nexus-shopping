package com.nexus.shopping.shipping.application.command

import java.math.BigDecimal

data class ProcessShippingCommand(
    val orderId: Long,
    val orderReference: String,
    val customer: ShippingCustomerSnapshot,
    val shippingAddress: ShippingAddressSnapshot,
    val items: List<ShippingItemSnapshot>,
    val totalAmount: BigDecimal,
)

data class ShippingCustomerSnapshot(
    val name: String,
)

data class ShippingAddressSnapshot(
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
)

data class ShippingItemSnapshot(
    val productId: Long,
    val productName: String,
    val quantity: Int,
)
