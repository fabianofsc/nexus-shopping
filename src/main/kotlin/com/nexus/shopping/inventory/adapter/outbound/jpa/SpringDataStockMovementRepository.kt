package com.nexus.shopping.inventory.adapter.outbound.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpringDataStockMovementRepository : JpaRepository<StockMovementEntity, Long> {
    @Modifying(clearAutomatically = true)
    @Query(
        value = """
            UPDATE products
            SET inventory_quantity = inventory_quantity - :quantity,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :productId
              AND inventory_quantity >= :quantity
        """,
        nativeQuery = true,
    )
    fun decrementIfAvailable(
        @Param("productId") productId: Long,
        @Param("quantity") quantity: Int,
    ): Int
}
