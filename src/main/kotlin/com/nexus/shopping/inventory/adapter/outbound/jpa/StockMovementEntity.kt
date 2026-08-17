package com.nexus.shopping.inventory.adapter.outbound.jpa

import com.nexus.shopping.inventory.domain.MovementType
import com.nexus.shopping.inventory.domain.StockMovement
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "stock_movements")
class StockMovementEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "product_id", nullable = false)
    var productId: Long = 0,
    @Column(name = "order_id")
    var orderId: Long? = null,
    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0,
    @Column(name = "movement_type", nullable = false, length = 16)
    var movementType: String = "",
    @Column(name = "reference", nullable = false, length = 255)
    var reference: String = "",
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.MIN,
) {
    fun toDomain(): StockMovement =
        StockMovement(
            id = requireNotNull(id) { "StockMovementEntity.id must be available before mapping to domain." },
            productId = productId,
            orderId = orderId,
            quantity = quantity,
            movementType = MovementType.valueOf(movementType),
            reference = reference,
            createdAt = createdAt,
        )
}

fun StockMovement.toNewEntity(): StockMovementEntity =
    StockMovementEntity(
        id = null,
        productId = productId,
        orderId = orderId,
        quantity = quantity,
        movementType = movementType.name,
        reference = reference,
        createdAt = createdAt,
    )
