package com.nexus.shopping.customer.application.port.inbound

import com.nexus.shopping.customer.domain.Customer

interface GetCustomerSnapshotInputPort {
    fun getSnapshot(customerId: Long): Customer?
}
