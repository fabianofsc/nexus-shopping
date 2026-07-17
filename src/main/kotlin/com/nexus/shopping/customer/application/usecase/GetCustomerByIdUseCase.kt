package com.nexus.shopping.customer.application.usecase

import com.nexus.shopping.customer.application.exception.CustomerNotFoundException
import com.nexus.shopping.customer.application.port.outbound.CustomerRepositoryPort
import com.nexus.shopping.customer.domain.Customer
import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GetCustomerByIdUseCase(
    private val customerRepository: CustomerRepositoryPort,
) {
    fun execute(id: Long): Customer {
        logger.infoWithContext("customer.get_by_id.started", "customer.id" to id)

        val customer = customerRepository.findById(id)
        if (customer == null) {
            logger.warnWithContext("customer.get_by_id.not_found", "customer.id" to id)
            throw CustomerNotFoundException("Customer $id not found.")
        }

        logger.infoWithContext("customer.get_by_id.completed", "customer.id" to customer.id)
        return customer
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(GetCustomerByIdUseCase::class.java)
    }
}
