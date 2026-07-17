package com.nexus.shopping.notification.adapter.outbound.jpa

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpringDataNotificationRepository : JpaRepository<NotificationEntity, Long> {
    @Query(
        """
        SELECT n FROM NotificationEntity n
        WHERE n.customerId = :customerId
        ORDER BY n.id
        """,
    )
    fun findByCustomerId(
        @Param("customerId") customerId: Long,
        pageable: Pageable,
    ): Slice<NotificationEntity>
}
