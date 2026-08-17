package com.nexus.shopping.inventory.adapter.outbound.jpa

import com.nexus.shopping.inventory.application.port.outbound.ProductStockPort
import com.nexus.shopping.inventory.application.port.outbound.StockLedgerPort
import com.nexus.shopping.inventory.domain.StockMovement
import org.springframework.stereotype.Repository

@Repository
class StockLedgerJpaRepositoryAdapter(
    private val repository: SpringDataStockMovementRepository,
) : StockLedgerPort,
    ProductStockPort {
    override fun record(movement: StockMovement) {
        repository.save(movement.toNewEntity())
    }

    override fun decrementIfAvailable(
        productId: Long,
        quantity: Int,
    ): Boolean = repository.decrementIfAvailable(productId, quantity) == 1

    override fun increment(
        productId: Long,
        quantity: Int,
    ) {
        repository.increment(productId, quantity)
    }
}
