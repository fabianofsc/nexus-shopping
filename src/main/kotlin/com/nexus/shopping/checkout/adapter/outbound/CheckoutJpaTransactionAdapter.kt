package com.nexus.shopping.checkout.adapter.outbound

import com.nexus.shopping.checkout.application.port.outbound.TransactionPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class CheckoutJpaTransactionAdapter : TransactionPort {
    @Transactional(propagation = Propagation.REQUIRED)
    override fun <T> inTransaction(block: () -> T): T = block()
}
