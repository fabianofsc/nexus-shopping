package com.nexus.shopping.product.adapter.outbound.jpa

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal

interface SpringDataProductRepository : JpaRepository<ProductEntity, Long> {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductEntity p SET p.priceAmount = :priceAmount, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    fun updatePriceById(
        @Param("id") id: Long,
        @Param("priceAmount") priceAmount: BigDecimal,
    ): Int

    @Query(
        """
        SELECT p FROM ProductEntity p
        WHERE p.categoryId = :categoryId
        ORDER BY p.id
        """,
    )
    fun findByCategoryId(
        @Param("categoryId") categoryId: Long,
        pageable: Pageable,
    ): Slice<ProductEntity>

    @Query(
        """
        SELECT p FROM ProductEntity p
        WHERE p.name >= :name
          AND p.name < :upperBound
          AND p.name LIKE :prefix
        ORDER BY p.name
        """,
    )
    fun findByNamePrefix(
        @Param("name") name: String,
        @Param("upperBound") upperBound: String,
        @Param("prefix") prefix: String,
        pageable: Pageable,
    ): Slice<ProductEntity>
}
