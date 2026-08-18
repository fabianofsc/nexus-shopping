package com.nexus.shopping.inventory.domain

import java.time.LocalDateTime

data class StockMovement(
    val id: Long?,
    val productId: Long,
    val orderId: Long?,
    val quantity: Int,
    val movementType: MovementType,
    val reference: String,
    val createdAt: LocalDateTime,
) {
    init {
        require(productId > 0) { "productId must be greater than zero." }
        require(quantity > 0) { "quantity must be greater than zero." }
        require(reference.isNotBlank()) { "reference must not be blank." }
        require(reference.length <= 255) { "reference must not exceed 255 characters." }
    }

    companion object {
        fun decrement(
            productId: Long,
            quantity: Int,
            reference: String,
            orderId: Long? = null,
            createdAt: LocalDateTime,
        ): StockMovement =
            StockMovement(
                id = null,
                productId = productId,
                orderId = orderId,
                quantity = quantity,
                movementType = MovementType.DECREASE,
                reference = reference,
                createdAt = createdAt,
            )

        fun release(
            productId: Long,
            quantity: Int,
            reference: String,
            orderId: Long? = null,
            createdAt: LocalDateTime,
        ): StockMovement =
            StockMovement(
                id = null,
                productId = productId,
                orderId = orderId,
                quantity = quantity,
                movementType = MovementType.RELEASE,
                reference = reference,
                createdAt = createdAt,
            )
    }
}
