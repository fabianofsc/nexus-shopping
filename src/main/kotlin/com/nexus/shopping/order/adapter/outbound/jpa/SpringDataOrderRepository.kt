package com.nexus.shopping.order.adapter.outbound.jpa

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface SpringDataOrderRepository : JpaRepository<OrderEntity, Long> {
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id")
    fun findOrderById(
        @Param("id") id: Long,
    ): Optional<OrderEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id")
    fun findOrderByIdForUpdate(
        @Param("id") id: Long,
    ): Optional<OrderEntity>

    @Query(
        """
        SELECT o FROM OrderEntity o
        WHERE o.customerId = :customerId AND o.idempotencyKey = :idempotencyKey
        """,
    )
    fun findByCustomerIdAndIdempotencyKey(
        @Param("customerId") customerId: Long,
        @Param("idempotencyKey") idempotencyKey: String,
    ): Optional<OrderEntity>

    @Query(
        """
        SELECT o FROM OrderEntity o
        WHERE o.customerId = :customerId
        ORDER BY o.createdAt DESC, o.id DESC
        """,
    )
    fun findByCustomerId(
        @Param("customerId") customerId: Long,
        pageable: Pageable,
    ): Slice<OrderEntity>
}
