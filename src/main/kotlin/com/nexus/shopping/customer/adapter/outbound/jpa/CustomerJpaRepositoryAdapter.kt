package com.nexus.shopping.customer.adapter.outbound.jpa

import com.nexus.shopping.customer.application.command.CreateCustomerCommand
import com.nexus.shopping.customer.application.port.outbound.CustomerRepositoryPort
import com.nexus.shopping.customer.domain.Customer
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class CustomerJpaRepositoryAdapter(
    private val repository: SpringDataCustomerRepository,
) : CustomerRepositoryPort {
    @Transactional(readOnly = true)
    override fun findById(id: Long): Customer? = repository.findCustomerById(id).orElse(null)?.toDomain()

    @Transactional
    override fun save(command: CreateCustomerCommand): Customer = repository.saveAndFlush(command.toEntity()).toDomain()
}
