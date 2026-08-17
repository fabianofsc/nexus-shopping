package com.nexus.shopping.order.adapter.outbound

import com.nexus.shopping.inventory.application.command.ReleaseStockCommand
import com.nexus.shopping.inventory.application.command.ReleaseStockItem
import com.nexus.shopping.inventory.application.port.inbound.ReleaseStockInputPort
import com.nexus.shopping.order.application.port.outbound.ReleaseStockPort
import com.nexus.shopping.order.application.port.outbound.ReleasedStockItem
import org.springframework.stereotype.Component

@Component
class InventoryReleaseStockAdapter(
    private val releaseStock: ReleaseStockInputPort,
) : ReleaseStockPort {
    override fun release(
        orderReference: String,
        items: List<ReleasedStockItem>,
    ) {
        releaseStock.release(
            ReleaseStockCommand(
                orderReference = orderReference,
                items = items.map { ReleaseStockItem(it.productId, it.quantity) },
            ),
        )
    }
}
