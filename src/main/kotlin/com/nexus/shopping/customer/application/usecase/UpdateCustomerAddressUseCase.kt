package com.nexus.shopping.customer.application.usecase

import com.nexus.shopping.customer.application.command.UpdateCustomerAddressCommand
import com.nexus.shopping.customer.application.exception.CustomerNotFoundException
import com.nexus.shopping.customer.application.exception.CustomerValidationException
import com.nexus.shopping.customer.application.port.outbound.CustomerRepositoryPort
import com.nexus.shopping.customer.domain.Address
import com.nexus.shopping.customer.domain.Customer
import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UpdateCustomerAddressUseCase(
    private val customerRepository: CustomerRepositoryPort,
) {
    fun execute(
        customerId: Long,
        command: UpdateCustomerAddressCommand,
    ): Customer {
        logger.infoWithContext("customer.address.update.started", "customer.id" to customerId)

        validate(command)

        val updated =
            customerRepository.updateAddress(customerId, command.toAddress())
                ?: throw CustomerNotFoundException("Customer $customerId not found.")

        logger.infoWithContext("customer.address.update.completed", "customer.id" to updated.id)
        return updated
    }

    private fun validate(command: UpdateCustomerAddressCommand) {
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
    }

    private fun requireNotBlank(
        value: String,
        fieldName: String,
    ) {
        if (value.isBlank()) invalid("$fieldName must not be blank.")
    }

    private fun requireMaxLength(
        value: String?,
        fieldName: String,
        maxLength: Int,
    ) {
        if (value != null && value.length > maxLength) {
            invalid("$fieldName must be at most $maxLength characters.")
        }
    }

    private fun invalid(message: String): Nothing {
        logger.warnWithContext("customer.address.update.validation_failed", "validation.error" to message)
        throw CustomerValidationException(message)
    }

    private fun UpdateCustomerAddressCommand.toAddress(): Address =
        Address(
            street = street,
            number = number,
            complement = complement,
            neighborhood = neighborhood,
            city = city,
            state = state,
            zipCode = zipCode,
            country = country,
        )

    private companion object {
        private val logger = LoggerFactory.getLogger(UpdateCustomerAddressUseCase::class.java)
    }
}
