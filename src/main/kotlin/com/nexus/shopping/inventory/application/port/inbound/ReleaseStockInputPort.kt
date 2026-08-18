package com.nexus.shopping.inventory.application.port.inbound

import com.nexus.shopping.inventory.application.command.ReleaseStockCommand

interface ReleaseStockInputPort {
    fun release(command: ReleaseStockCommand)
}
