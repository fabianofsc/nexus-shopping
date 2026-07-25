package com.nexus.shopping.order.application.command

import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.ShippingAddressSnapshot

data class CheckoutOrderCommand(
    val customerId: Long,
    val customerSnapshot: CustomerSnapshot,
    val shippingAddressSnapshot: ShippingAddressSnapshot,
    val idempotencyKey: String,
)
