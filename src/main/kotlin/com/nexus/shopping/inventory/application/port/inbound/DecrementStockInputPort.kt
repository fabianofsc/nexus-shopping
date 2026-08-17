package com.nexus.shopping.inventory.application.port.inbound

import com.nexus.shopping.inventory.application.command.DecrementStockCommand

interface DecrementStockInputPort {
    fun decrement(command: DecrementStockCommand)
}
