package com.nexus.shopping.inventory

import com.nexus.shopping.inventory.application.command.ReleaseStockCommand
import com.nexus.shopping.inventory.application.command.ReleaseStockItem
import com.nexus.shopping.inventory.application.exception.InventoryValidationException
import com.nexus.shopping.inventory.application.port.outbound.ProductStockPort
import com.nexus.shopping.inventory.application.port.outbound.StockLedgerPort
import com.nexus.shopping.inventory.application.usecase.ReleaseStockUseCase
import com.nexus.shopping.inventory.domain.MovementType
import com.nexus.shopping.inventory.domain.StockMovement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReleaseStockUseCaseTest {
    @Test
    fun `increments stock and records one RELEASE movement per item`() {
        val productStock = ReleaseProductStockFake()
        val stockLedger = ReleaseStockLedgerFake()

        ReleaseStockUseCase(productStock, stockLedger).release(
            ReleaseStockCommand(
                orderReference = "order-1",
                items = listOf(ReleaseStockItem(1, 2), ReleaseStockItem(2, 1)),
            ),
        )

        assertEquals(listOf(1L to 2, 2L to 1), productStock.increments)
        assertEquals(2, stockLedger.movements.size)
        assertTrue(stockLedger.movements.all { it.movementType == MovementType.RELEASE })
        assertTrue(stockLedger.movements.all { it.reference == "order-1" })
        assertEquals(listOf(1L, 2L), stockLedger.movements.map { it.productId })
    }

    @Test
    fun `validates command inputs before any effect`() {
        val useCase = ReleaseStockUseCase(ReleaseProductStockFake(), ReleaseStockLedgerFake())

        assertFailsWith<InventoryValidationException> {
            useCase.release(ReleaseStockCommand(" ", listOf(ReleaseStockItem(1, 1))))
        }
        assertFailsWith<InventoryValidationException> {
            useCase.release(ReleaseStockCommand("order-1", emptyList()))
        }
        assertFailsWith<InventoryValidationException> {
            useCase.release(ReleaseStockCommand("order-1", listOf(ReleaseStockItem(0, 1))))
        }
        assertFailsWith<InventoryValidationException> {
            useCase.release(ReleaseStockCommand("order-1", listOf(ReleaseStockItem(1, 0))))
        }
    }
}

private class ReleaseProductStockFake : ProductStockPort {
    val increments = mutableListOf<Pair<Long, Int>>()

    override fun decrementIfAvailable(
        productId: Long,
        quantity: Int,
    ): Boolean = error("Not used by release")

    override fun increment(
        productId: Long,
        quantity: Int,
    ) {
        increments += productId to quantity
    }
}

private class ReleaseStockLedgerFake : StockLedgerPort {
    val movements = mutableListOf<StockMovement>()

    override fun record(movement: StockMovement) {
        movements += movement
    }
}
