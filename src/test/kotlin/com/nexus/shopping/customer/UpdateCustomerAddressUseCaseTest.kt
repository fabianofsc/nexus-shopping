package com.nexus.shopping.customer

import com.nexus.shopping.customer.application.command.CreateCustomerCommand
import com.nexus.shopping.customer.application.command.UpdateCustomerAddressCommand
import com.nexus.shopping.customer.application.exception.CustomerNotFoundException
import com.nexus.shopping.customer.application.exception.CustomerValidationException
import com.nexus.shopping.customer.application.port.outbound.CustomerRepositoryPort
import com.nexus.shopping.customer.application.usecase.UpdateCustomerAddressUseCase
import com.nexus.shopping.customer.domain.Address
import com.nexus.shopping.customer.domain.Contact
import com.nexus.shopping.customer.domain.Customer
import com.nexus.shopping.customer.domain.CustomerStatus
import com.nexus.shopping.customer.domain.DocumentType
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateCustomerAddressUseCaseTest {
    private var savedAddress: Address? = null
    private val fakeRepo =
        object : CustomerRepositoryPort {
            override fun findById(id: Long): Customer? = throw UnsupportedOperationException()

            override fun save(command: CreateCustomerCommand): Customer = throw UnsupportedOperationException()

            override fun updateAddress(
                customerId: Long,
                address: Address,
            ): Customer? {
                if (customerId != 1L) return null
                savedAddress = address
                return aCustomer(address = address)
            }
        }

    private val useCase = UpdateCustomerAddressUseCase(fakeRepo)

    @Test
    fun `overwrites the customer address`() {
        val customer = useCase.execute(1L, validCommand())

        assertEquals("Av. Paulista", customer.address.street)
        assertEquals("Sao Paulo", customer.address.city)
        assertEquals("Av. Paulista", savedAddress?.street)
    }

    @Test
    fun `throws CustomerNotFoundException when customer does not exist`() {
        assertFailsWith<CustomerNotFoundException> {
            useCase.execute(999L, validCommand())
        }
    }

    @Test
    fun `throws CustomerValidationException when address is incomplete`() {
        assertFailsWith<CustomerValidationException> {
            useCase.execute(1L, validCommand().copy(city = ""))
        }
    }

    @Test
    fun `throws CustomerValidationException when a field exceeds its maximum length`() {
        assertFailsWith<CustomerValidationException> {
            useCase.execute(1L, validCommand().copy(street = "X".repeat(181)))
        }
    }

    private fun validCommand() =
        UpdateCustomerAddressCommand(
            street = "Av. Paulista",
            number = "1000",
            complement = null,
            neighborhood = "Bela Vista",
            city = "Sao Paulo",
            state = "SP",
            zipCode = "01310000",
            country = "BR",
        )

    private fun aCustomer(address: Address) =
        Customer(
            id = 1L,
            name = "Ana Silva",
            document = "02648629025",
            documentType = DocumentType.CPF,
            status = CustomerStatus.ACTIVE,
            contact = Contact("ana.silva@example.com", null),
            address = address,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
}
