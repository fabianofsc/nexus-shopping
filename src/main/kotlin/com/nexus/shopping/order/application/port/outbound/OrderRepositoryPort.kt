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
     * Stores [order] after the caller has checked replay and acquired the checkout cart lock.
     * A database uniqueness violation is deliberately propagated: recovering it in this same
     * transaction is invalid on PostgreSQL because that transaction is already aborted.
     */
    fun createIfAbsentByCustomerIdAndIdempotencyKey(order: Order): Order

    /**
     * Returns a newly persisted order. Replays are resolved by the caller before this method is
     * reached, while it holds the cart lock; the default keeps simple fakes compatible.
     */
    fun createIfAbsentWithResultByCustomerIdAndIdempotencyKey(order: Order): OrderCreationResult {
        val existing = findByCustomerIdAndIdempotencyKey(order.customerId, order.idempotencyKey)
        if (existing != null) return OrderCreationResult(existing, created = false)
        return OrderCreationResult(createIfAbsentByCustomerIdAndIdempotencyKey(order), created = true)
    }

    fun update(order: Order): Order
}

data class OrderCreationResult(
    val order: Order,
    val created: Boolean,
)
