package com.nexus.shopping.inventory.domain

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StockMovementTest {
    private val createdAt = LocalDateTime.of(2026, 8, 17, 10, 0)

    @Test
    fun `decrement factory creates a DECREASE movement`() {
        val movement =
            StockMovement.decrement(
                productId = 1,
                quantity = 2,
                reference = "order-1",
                createdAt = createdAt,
            )

        assertEquals(MovementType.DECREASE, movement.movementType)
        assertEquals(2, movement.quantity)
        assertEquals("order-1", movement.reference)
    }

    @Test
    fun `release factory creates a RELEASE movement`() {
        val movement =
            StockMovement.release(
                productId = 1,
                quantity = 2,
                reference = "order-1",
                createdAt = createdAt,
            )

        assertEquals(MovementType.RELEASE, movement.movementType)
    }

    @Test
    fun `rejects non positive quantity`() {
        assertFailsWith<IllegalArgumentException> {
            StockMovement.decrement(productId = 1, quantity = 0, reference = "order-1", createdAt = createdAt)
        }
        assertFailsWith<IllegalArgumentException> {
            StockMovement.release(productId = 1, quantity = -1, reference = "order-1", createdAt = createdAt)
        }
    }

    @Test
    fun `rejects blank reference`() {
        assertFailsWith<IllegalArgumentException> {
            StockMovement.decrement(productId = 1, quantity = 1, reference = " ", createdAt = createdAt)
        }
    }

    @Test
    fun `rejects non positive product id`() {
        assertFailsWith<IllegalArgumentException> {
            StockMovement.decrement(productId = 0, quantity = 1, reference = "order-1", createdAt = createdAt)
        }
    }
}
