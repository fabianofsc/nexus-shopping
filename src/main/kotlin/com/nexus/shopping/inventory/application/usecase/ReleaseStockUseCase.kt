package com.nexus.shopping.inventory.application.usecase

import com.nexus.shopping.inventory.application.command.ReleaseStockCommand
import com.nexus.shopping.inventory.application.exception.InventoryValidationException
import com.nexus.shopping.inventory.application.port.inbound.ReleaseStockInputPort
import com.nexus.shopping.inventory.application.port.outbound.ProductStockPort
import com.nexus.shopping.inventory.application.port.outbound.StockLedgerPort
import com.nexus.shopping.inventory.application.port.outbound.TransactionPort
import com.nexus.shopping.inventory.domain.StockMovement
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ReleaseStockUseCase(
    private val productStock: ProductStockPort,
    private val stockLedger: StockLedgerPort,
    private val transaction: TransactionPort = NoopTransaction,
) : ReleaseStockInputPort {
    override fun release(command: ReleaseStockCommand) {
        validate(command)
        transaction.inTransaction {
            command.items.forEach { item ->
                productStock.increment(item.productId, item.quantity)
                stockLedger.record(
                    StockMovement.release(
                        productId = item.productId,
                        quantity = item.quantity,
                        reference = command.orderReference,
                        createdAt = LocalDateTime.now(),
                    ),
                )
            }
        }
    }

    private fun validate(command: ReleaseStockCommand) {
        if (command.orderReference.isBlank()) invalid("orderReference must not be blank.")
        if (command.orderReference.length > 255) invalid("orderReference must not exceed 255 characters.")
        if (command.items.isEmpty()) invalid("items must not be empty.")
        command.items.forEachIndexed { index, item ->
            if (item.productId <= 0) invalid("items[$index].productId must be greater than zero.")
            if (item.quantity <= 0) invalid("items[$index].quantity must be greater than zero.")
        }
    }

    private fun invalid(message: String): Nothing = throw InventoryValidationException(message)
}

private object NoopTransaction : TransactionPort {
    override fun <T> inTransaction(block: () -> T): T = block()
}
