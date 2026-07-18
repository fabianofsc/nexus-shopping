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
     *
     * Deliberately does NOT add `LEFT JOIN FETCH c.items` here (unlike findByCustomerIdAndStatus):
     * combining PESSIMISTIC_WRITE with an outer join against a *-to-many collection made the lock
     * silently ineffective under H2 in this project (CartConcurrencyTest's lost-update assertion
     * started failing deterministically once the join was added, then passed again once removed).
     * Items are still loaded eagerly via CartEntity's `fetch = EAGER` mapping, as a second SELECT
     * within the same locked transaction - one extra round-trip per call, but a lock that actually
     * locks. See PR #21 review discussion for the empirical finding.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CartEntity c WHERE c.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): Optional<CartEntity>
}
