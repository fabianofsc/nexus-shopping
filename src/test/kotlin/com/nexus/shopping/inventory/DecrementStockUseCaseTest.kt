package com.nexus.shopping.inventory

import com.nexus.shopping.inventory.application.command.DecrementStockCommand
import com.nexus.shopping.inventory.application.command.DecrementStockItem
import com.nexus.shopping.inventory.application.exception.InsufficientStockException
import com.nexus.shopping.inventory.application.exception.InventoryValidationException
import com.nexus.shopping.inventory.application.port.outbound.ProductStockPort
import com.nexus.shopping.inventory.application.port.outbound.StockLedgerPort
import com.nexus.shopping.inventory.application.usecase.DecrementStockUseCase
import com.nexus.shopping.inventory.domain.MovementType
import com.nexus.shopping.inventory.domain.StockMovement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DecrementStockUseCaseTest {
    @Test
    fun `decrements every item and records one DECREASE movement per item`() {
        val productStock = ProductStockFake()
        val stockLedger = StockLedgerFake()

        DecrementStockUseCase(productStock, stockLedger).decrement(
            DecrementStockCommand(
                orderReference = "order-1",
                items = listOf(DecrementStockItem(1, 2), DecrementStockItem(2, 1)),
            ),
        )

        assertEquals(listOf(1L to 2, 2L to 1), productStock.decrements)
        assertEquals(2, stockLedger.movements.size)
        assertTrue(stockLedger.movements.all { it.movementType == MovementType.DECREASE })
        assertTrue(stockLedger.movements.all { it.reference == "order-1" })
        assertEquals(listOf(1L, 2L), stockLedger.movements.map { it.productId })
    }

    @Test
    fun `fails with insufficient stock and does not record a movement for the failing item`() {
        val productStock = ProductStockFake(available = mapOf(1L to 5, 2L to 0))
        val stockLedger = StockLedgerFake()

        assertFailsWith<InsufficientStockException> {
            DecrementStockUseCase(productStock, stockLedger).decrement(
                DecrementStockCommand(
                    orderReference = "order-1",
                    items = listOf(DecrementStockItem(1, 5), DecrementStockItem(2, 1)),
                ),
            )
        }

        assertEquals(listOf(1L to 5), productStock.decrements)
        assertEquals(1, stockLedger.movements.size)
    }

    @Test
    fun `validates command inputs before any effect`() {
        val useCase = DecrementStockUseCase(ProductStockFake(), StockLedgerFake())

        assertFailsWith<InventoryValidationException> {
            useCase.decrement(DecrementStockCommand(" ", listOf(DecrementStockItem(1, 1))))
        }
        assertFailsWith<InventoryValidationException> {
            useCase.decrement(DecrementStockCommand("order-1", emptyList()))
        }
        assertFailsWith<InventoryValidationException> {
            useCase.decrement(DecrementStockCommand("order-1", listOf(DecrementStockItem(0, 1))))
        }
        assertFailsWith<InventoryValidationException> {
            useCase.decrement(DecrementStockCommand("order-1", listOf(DecrementStockItem(1, 0))))
        }
    }
}

private class ProductStockFake(
    var available: Map<Long, Int> = mapOf(1L to 100, 2L to 100),
) : ProductStockPort {
    val decrements = mutableListOf<Pair<Long, Int>>()

    override fun decrementIfAvailable(
        productId: Long,
        quantity: Int,
    ): Boolean {
        val stock = available[productId] ?: return false
        if (stock < quantity) return false
        available = available + (productId to (stock - quantity))
        decrements += productId to quantity
        return true
    }

    override fun increment(
        productId: Long,
        quantity: Int,
    ) = error("Not used by decrement")
}

private class StockLedgerFake : StockLedgerPort {
    val movements = mutableListOf<StockMovement>()

    override fun record(movement: StockMovement) {
        movements += movement
    }
}
