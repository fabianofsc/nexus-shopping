package com.nexus.shopping.order.application.command

import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.ShippingAddressSnapshot

data class CreateOrderCommand(
    val customerId: Long,
    val cartId: Long,
    val customerSnapshot: CustomerSnapshot,
    val shippingAddressSnapshot: ShippingAddressSnapshot,
    val items: List<OrderItemSnapshot>,
    val idempotencyKey: String,
)
