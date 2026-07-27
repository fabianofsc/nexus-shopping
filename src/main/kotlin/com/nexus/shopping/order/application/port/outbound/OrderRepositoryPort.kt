package com.nexus.shopping.order.application.port.outbound

import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.platform.domain.PageResult

interface OrderRepositoryPort {
    fun findById(id: Long): Order?

    /**
     * Returns the current order while holding a write lock until the surrounding transaction
     * finishes. The default preserves the lightweight fake implementations used by unit tests.
     */
    fun findByIdForUpdate(id: Long): Order? = findById(id)

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
     * Atomically stores [order] or returns the order that already owns the same customer and
     * idempotency key. Implementations must serialize that decision without exposing a database
     * uniqueness violation to the application layer.
     */
    fun create(order: Order): OrderPersistenceResult

    fun update(order: Order): Order
}

data class OrderPersistenceResult(
    val order: Order,
    val created: Boolean,
)
