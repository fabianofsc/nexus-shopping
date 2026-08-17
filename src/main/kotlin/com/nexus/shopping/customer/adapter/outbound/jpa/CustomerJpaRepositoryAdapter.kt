package com.nexus.shopping.customer.adapter.outbound.jpa

import com.nexus.shopping.customer.application.command.CreateCustomerCommand
import com.nexus.shopping.customer.application.port.outbound.CustomerRepositoryPort
import com.nexus.shopping.customer.domain.Address
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

    @Transactional
    override fun updateAddress(
        customerId: Long,
        address: Address,
    ): Customer? {
        val entity = repository.findCustomerById(customerId).orElse(null) ?: return null
        val addressEntity = requireNotNull(entity.address) { "CustomerEntity.address must be available before updating." }
        addressEntity.street = address.street
        addressEntity.number = address.number
        addressEntity.complement = address.complement
        addressEntity.neighborhood = address.neighborhood
        addressEntity.city = address.city
        addressEntity.state = address.state
        addressEntity.zipCode = address.zipCode
        addressEntity.country = address.country
        return repository.saveAndFlush(entity).toDomain()
    }
}
