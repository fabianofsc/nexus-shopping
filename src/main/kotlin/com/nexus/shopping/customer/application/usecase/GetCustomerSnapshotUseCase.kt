package com.nexus.shopping.customer.application.usecase

import com.nexus.shopping.customer.application.port.inbound.GetCustomerSnapshotInputPort
import com.nexus.shopping.customer.application.port.outbound.CustomerRepositoryPort
import com.nexus.shopping.customer.domain.Customer
import org.springframework.stereotype.Service

@Service
class GetCustomerSnapshotUseCase(
    private val customerRepository: CustomerRepositoryPort,
) : GetCustomerSnapshotInputPort {
    override fun getSnapshot(customerId: Long): Customer? = customerRepository.findById(customerId)
}
