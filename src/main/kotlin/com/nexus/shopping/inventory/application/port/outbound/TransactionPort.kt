package com.nexus.shopping.inventory.application.port.outbound

interface TransactionPort {
    fun <T> inTransaction(block: () -> T): T
}
