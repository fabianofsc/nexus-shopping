package com.nexus.shopping.cart.adapter.outbound.jpa

import com.nexus.shopping.cart.domain.CartStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface SpringDataCartRepository : JpaRepository<CartEntity, Long> {
    @Query(
        """
        SELECT c FROM CartEntity c
        LEFT JOIN FETCH c.items
        WHERE c.customerId = :customerId AND c.status = :status
        """,
    )
    fun findByCustomerIdAndStatus(
        @Param("customerId") customerId: Long,
        @Param("status") status: CartStatus,
    ): Optional<CartEntity>

    /**
     * Locks the cart row (`SELECT ... FOR UPDATE`, portable between H2 and PostgreSQL) for the
     * duration of the caller's transaction, so concurrent read-modify-write cycles on the same
     * cart (add/remove/clear item) serialize instead of racing on a stale in-memory item list.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CartEntity c WHERE c.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): Optional<CartEntity>
}
