package com.nexus.shopping.order.application.port.inbound

import com.nexus.shopping.order.application.command.CreateOrderCommand
import com.nexus.shopping.order.domain.Order

interface CreateOrderInputPort {
    fun create(command: CreateOrderCommand): CreatedOrder
}

data class CreatedOrder(
    val order: Order,
    val replayed: Boolean,
)
