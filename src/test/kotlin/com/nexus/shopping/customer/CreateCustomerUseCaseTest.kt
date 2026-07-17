package com.nexus.shopping.customer

import com.nexus.shopping.customer.application.command.CreateCustomerCommand
import com.nexus.shopping.customer.application.exception.CustomerValidationException
import com.nexus.shopping.customer.application.port.outbound.CustomerRepositoryPort
import com.nexus.shopping.customer.application.usecase.CreateCustomerUseCase
import com.nexus.shopping.customer.domain.Address
import com.nexus.shopping.customer.domain.Contact
import com.nexus.shopping.customer.domain.Customer
import com.nexus.shopping.customer.domain.DocumentType
import com.nexus.shopping.customer.domain.CustomerStatus
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateCustomerUseCaseTest {
    private val fakeRepo =
        object : CustomerRepositoryPort {
            override fun findById(id: Long): Customer? = throw UnsupportedOperationException()

            override fun save(command: CreateCustomerCommand): Customer =
                Customer(
                    id = 1L,
                    name = command.name,
                    document = command.document,
                    documentType = DocumentType.valueOf(command.documentType),
                    status = CustomerStatus.ACTIVE,
                    contact = Contact(command.email, command.phone),
                    address =
                        Address(
                            street = command.street,
                            number = command.number,
                            complement = command.complement,
                            neighborhood = command.neighborhood,
                            city = command.city,
                            state = command.state,
                            zipCode = command.zipCode,
                            country = command.country,
                        ),
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )
        }

    private val useCase = CreateCustomerUseCase(fakeRepo)

    private fun validCommand() =
        CreateCustomerCommand(
            name = "Ana Silva",
            document = "02648629025",
            documentType = "CPF",
            email = "ana.silva@example.com",
            phone = "+5511999990000",
            street = "Rua das Flores",
            number = "123",
            complement = "Apto 45",
            neighborhood = "Centro",
            city = "Sao Paulo",
            state = "SP",
            zipCode = "01001000",
            country = "BR",
        )

    @Test
    fun `creates customer with active status`() {
        val customer = useCase.create(validCommand())

        assertEquals(1L, customer.id)
        assertEquals("Ana Silva", customer.name)
        assertEquals(DocumentType.CPF, customer.documentType)
        assertEquals(CustomerStatus.ACTIVE, customer.status)
        assertEquals("ana.silva@example.com", customer.contact.email)
        assertEquals("Sao Paulo", customer.address.city)
    }

    @Test
    fun `throws CustomerValidationException when name is blank`() {
        assertFailsWith<CustomerValidationException> {
            useCase.create(validCommand().copy(name = ""))
        }
    }

    @Test
    fun `throws CustomerValidationException when email is invalid`() {
        assertFailsWith<CustomerValidationException> {
            useCase.create(validCommand().copy(email = "invalid-email"))
        }
    }

    @Test
    fun `throws CustomerValidationException when document type is invalid`() {
        assertFailsWith<CustomerValidationException> {
            useCase.create(validCommand().copy(documentType = "PASSPORT"))
        }
    }

    @Test
    fun `throws CustomerValidationException when address is incomplete`() {
        assertFailsWith<CustomerValidationException> {
            useCase.create(validCommand().copy(city = ""))
        }
    }
}
