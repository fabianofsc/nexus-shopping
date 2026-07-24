package com.nexus.shopping.order.adapter.outbound.jpa

import com.nexus.shopping.order.application.port.outbound.TransactionPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class JpaTransactionAdapter : TransactionPort {
    @Transactional
    override fun <T> inTransaction(block: () -> T): T = block()
}
