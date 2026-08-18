package com.nexus.shopping.inventory.application.port.outbound

import com.nexus.shopping.inventory.domain.StockMovement

interface StockLedgerPort {
    fun record(movement: StockMovement)
}
