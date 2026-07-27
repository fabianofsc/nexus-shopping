package com.nexus.shopping.integration.checkout.adapter.outbound

import com.nexus.shopping.integration.checkout.application.port.outbound.TransactionPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CheckoutJpaTransactionAdapter : TransactionPort {
    @Transactional
    override fun <T> inTransaction(block: () -> T): T = block()
}
