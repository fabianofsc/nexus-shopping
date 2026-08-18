package com.nexus.shopping.checkout.adapter.outbound.acl

import com.nexus.shopping.checkout.application.model.CheckoutItemSnapshot
import com.nexus.shopping.checkout.application.port.outbound.InventoryGateway
import com.nexus.shopping.inventory.application.command.DecrementStockCommand
import com.nexus.shopping.inventory.application.command.DecrementStockItem
import com.nexus.shopping.inventory.application.command.ReleaseStockCommand
import com.nexus.shopping.inventory.application.command.ReleaseStockItem
import com.nexus.shopping.inventory.application.port.inbound.DecrementStockInputPort
import com.nexus.shopping.inventory.application.port.inbound.ReleaseStockInputPort
import org.springframework.stereotype.Component

@Component
class InventoryGatewayAdapter(
    private val decrementStock: DecrementStockInputPort,
    private val releaseStock: ReleaseStockInputPort,
) : InventoryGateway {
    override fun decrement(
        orderReference: String,
        items: List<CheckoutItemSnapshot>,
    ) {
        decrementStock.decrement(
            DecrementStockCommand(
                orderReference = orderReference,
                items = items.map { DecrementStockItem(it.productId, it.quantity) },
            ),
        )
    }

    override fun release(
        orderReference: String,
        items: List<CheckoutItemSnapshot>,
    ) {
        releaseStock.release(
            ReleaseStockCommand(
                orderReference = orderReference,
                items = items.map { ReleaseStockItem(it.productId, it.quantity) },
            ),
        )
    }
}
