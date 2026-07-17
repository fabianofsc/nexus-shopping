package com.nexus.shopping.customer.application.usecase

import com.nexus.shopping.customer.application.exception.CustomerNotFoundException
import com.nexus.shopping.customer.application.port.outbound.CustomerRepositoryPort
import com.nexus.shopping.customer.domain.Customer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GetCustomerByIdUseCase(
    private val customerRepository: CustomerRepositoryPort,
) {
    fun execute(id: Long): Customer {
        logger.info("customer.get_by_id.started customer.id={}", id)
        val customer = customerRepository.findById(id) ?: throw CustomerNotFoundException("Customer $id not found.")
        logger.info("customer.get_by_id.completed customer.id={}", customer.id)
        return customer
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(GetCustomerByIdUseCase::class.java)
    }
}
