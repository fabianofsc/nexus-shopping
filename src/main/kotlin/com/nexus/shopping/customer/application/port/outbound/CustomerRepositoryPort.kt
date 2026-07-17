package com.nexus.shopping.customer.application.port.outbound

import com.nexus.shopping.customer.application.command.CreateCustomerCommand
import com.nexus.shopping.customer.domain.Customer

interface CustomerRepositoryPort {
    fun findById(id: Long): Customer?

    fun save(command: CreateCustomerCommand): Customer
}
