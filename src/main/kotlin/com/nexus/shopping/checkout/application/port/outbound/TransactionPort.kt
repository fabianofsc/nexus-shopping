package com.nexus.shopping.checkout.application.port.outbound

interface TransactionPort {
    fun <T> inTransaction(block: () -> T): T
}
