package com.nexus.shopping.customer.application.usecase

import com.nexus.shopping.customer.application.command.CreateCustomerCommand
import com.nexus.shopping.customer.application.exception.CustomerValidationException
import com.nexus.shopping.customer.application.port.outbound.CustomerRepositoryPort
import com.nexus.shopping.customer.domain.Customer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CreateCustomerUseCase(
    private val customerRepository: CustomerRepositoryPort,
) {
    fun create(command: CreateCustomerCommand): Customer {
        logger.info("customer.create.started")

        requireNotBlank(command.name, "name")
        requireMaxLength(command.name, "name", 160)
        requireNotBlank(command.document, "document")
        requireMaxLength(command.document, "document", 32)
        requireNotBlank(command.email, "email")
        requireMaxLength(command.email, "email", 254)
        if (!command.email.contains("@")) throwValidationFailed("email must be valid.")
        requireMaxLength(command.phone, "phone", 32)
        requireNotBlank(command.street, "street")
        requireMaxLength(command.street, "street", 180)
        requireNotBlank(command.number, "number")
        requireMaxLength(command.number, "number", 40)
        requireMaxLength(command.complement, "complement", 120)
        requireNotBlank(command.neighborhood, "neighborhood")
        requireMaxLength(command.neighborhood, "neighborhood", 120)
        requireNotBlank(command.city, "city")
        requireMaxLength(command.city, "city", 120)
        requireNotBlank(command.state, "state")
        requireMaxLength(command.state, "state", 60)
        requireNotBlank(command.zipCode, "zipCode")
        requireMaxLength(command.zipCode, "zipCode", 20)
        requireNotBlank(command.country, "country")
        requireMaxLength(command.country, "country", 2)

        val customer = customerRepository.save(command)
        logger.info("customer.create.completed customer.id={}", customer.id)
        return customer
    }

    private fun requireNotBlank(
        value: String,
        fieldName: String,
    ) {
        if (value.isBlank()) throwValidationFailed("$fieldName must not be blank.")
    }

    private fun requireMaxLength(
        value: String?,
        fieldName: String,
        maxLength: Int,
    ) {
        if (value != null && value.length > maxLength) {
            throwValidationFailed("$fieldName must be at most $maxLength characters.")
        }
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warn("customer.create.validation_failed validation.error={}", message)
        throw CustomerValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(CreateCustomerUseCase::class.java)
    }
}
