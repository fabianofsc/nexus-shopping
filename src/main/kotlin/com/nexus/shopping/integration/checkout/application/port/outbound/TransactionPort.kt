package com.nexus.shopping.integration.checkout.application.port.outbound

interface TransactionPort {
    fun <T> inTransaction(block: () -> T): T
}
