package com.nexus.shopping.inventory.adapter.outbound.jpa

import com.nexus.shopping.inventory.application.port.outbound.TransactionPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class InventoryJpaTransactionAdapter : TransactionPort {
    @Transactional
    override fun <T> inTransaction(block: () -> T): T = block()
}
