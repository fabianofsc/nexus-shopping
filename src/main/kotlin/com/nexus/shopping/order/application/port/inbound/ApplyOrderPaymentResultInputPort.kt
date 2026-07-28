package com.nexus.shopping.order.application.port.inbound

import com.nexus.shopping.order.application.command.ApplyOrderPaymentResultCommand
import com.nexus.shopping.order.domain.Order

interface ApplyOrderPaymentResultInputPort {
    fun apply(command: ApplyOrderPaymentResultCommand): Order
}
