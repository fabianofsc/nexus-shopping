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

    /**
     * Preserves the original insert-or-replay contract while allowing callers that expose HTTP
     * semantics to distinguish a new order from an idempotent replay. Adapters with a database
     * unique constraint must override this atomically; the default keeps simple fakes compatible.
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
