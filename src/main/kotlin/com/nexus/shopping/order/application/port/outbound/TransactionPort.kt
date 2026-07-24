package com.nexus.shopping.order.application.port.outbound

/** Executes [block] in the adapter's transaction boundary without coupling application code to Spring. */
interface TransactionPort {
    fun <T> inTransaction(block: () -> T): T
}
