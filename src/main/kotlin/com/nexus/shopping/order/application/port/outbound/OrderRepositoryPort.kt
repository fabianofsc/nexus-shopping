package com.nexus.shopping.order.application.port.outbound

import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.platform.domain.PageResult

interface OrderRepositoryPort {
    fun findById(id: Long): Order?

    fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): Order?

    fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Order>

    /**
     * Atomically stores [order] only when its (customerId, idempotencyKey) is new. When that key
     * already exists, returns the stored order. The caller compares the fingerprint to distinguish
     * an idempotent replay from a conflicting reuse of the key.
     */
    fun createIfAbsentByCustomerIdAndIdempotencyKey(order: Order): Order

    fun update(order: Order): Order
}
