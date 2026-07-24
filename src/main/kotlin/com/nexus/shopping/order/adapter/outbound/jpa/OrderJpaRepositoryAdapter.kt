package com.nexus.shopping.order.adapter.outbound.jpa

import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.platform.domain.PageResult
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class OrderJpaRepositoryAdapter(
    private val repository: SpringDataOrderRepository,
) : OrderRepositoryPort {
    @Transactional(readOnly = true)
    override fun findById(id: Long): Order? = repository.findOrderById(id).orElse(null)?.toDomain()

    @Transactional
    override fun findByIdForUpdate(id: Long): Order? = repository.findOrderByIdForUpdate(id).orElse(null)?.toDomain()

    @Transactional(readOnly = true)
    override fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): Order? = repository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey).orElse(null)?.toDomain()

    @Transactional(readOnly = true)
    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Order> {
        val slice = repository.findByCustomerId(customerId, PageRequest.of(page, size))
        val content = slice.content.map { it.toDomain() }
        return PageResult(content, page, size, content.size, slice.hasNext())
    }

    @Transactional
    override fun create(order: Order): Order = repository.saveAndFlush(order.toEntity()).toDomain()

    @Transactional
    override fun update(order: Order): Order {
        val entity = repository.findOrderById(requireNotNull(order.id) { "Order.id is required for update." }).orElseThrow()
        entity.status = order.status
        entity.cancelledAt = order.cancelledAt
        return repository.saveAndFlush(entity).toDomain()
    }
}
