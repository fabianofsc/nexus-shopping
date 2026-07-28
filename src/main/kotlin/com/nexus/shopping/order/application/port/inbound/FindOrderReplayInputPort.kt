package com.nexus.shopping.order.application.port.inbound

import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.ShippingAddressSnapshot

interface FindOrderReplayInputPort {
    fun findReplay(command: FindOrderReplayCommand): CreatedOrder?
}

data class FindOrderReplayCommand(
    val customerId: Long,
    val customerSnapshot: CustomerSnapshot,
    val shippingAddressSnapshot: ShippingAddressSnapshot,
    val idempotencyKey: String,
    val paymentAuthorizationFingerprint: String,
)
