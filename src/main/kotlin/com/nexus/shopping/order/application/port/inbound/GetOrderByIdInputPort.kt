package com.nexus.shopping.order.application.port.inbound

import com.nexus.shopping.order.domain.Order

interface GetOrderByIdInputPort {
    fun execute(id: Long): Order
}
