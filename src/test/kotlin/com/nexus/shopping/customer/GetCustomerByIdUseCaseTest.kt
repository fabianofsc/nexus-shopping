package com.nexus.shopping.customer

import com.nexus.shopping.customer.application.command.CreateCustomerCommand
import com.nexus.shopping.customer.application.exception.CustomerNotFoundException
import com.nexus.shopping.customer.application.port.outbound.CustomerRepositoryPort
import com.nexus.shopping.customer.application.usecase.GetCustomerByIdUseCase
import com.nexus.shopping.customer.domain.Address
import com.nexus.shopping.customer.domain.Contact
import com.nexus.shopping.customer.domain.Customer
import com.nexus.shopping.customer.domain.CustomerStatus
import com.nexus.shopping.customer.domain.DocumentType
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetCustomerByIdUseCaseTest {
    private var repoReturn: Customer? = null
    private val fakeRepo =
        object : CustomerRepositoryPort {
            override fun findById(id: Long): Customer? = repoReturn

            override fun save(command: CreateCustomerCommand): Customer = throw UnsupportedOperationException()
        }

    private val useCase = GetCustomerByIdUseCase(fakeRepo)

    @Test
    fun `returns customer when it exists`() {
        repoReturn = aCustomer()

        val customer = useCase.execute(1L)

        assertEquals(1L, customer.id)
        assertEquals("Ana Silva", customer.name)
        assertEquals(DocumentType.CPF, customer.documentType)
    }

    @Test
    fun `throws CustomerNotFoundException when customer does not exist`() {
        repoReturn = null

        assertFailsWith<CustomerNotFoundException> {
            useCase.execute(999L)
        }
    }

    private fun aCustomer() =
        Customer(
            id = 1L,
            name = "Ana Silva",
            document = "02648629025",
            documentType = DocumentType.CPF,
            status = CustomerStatus.ACTIVE,
            contact = Contact("ana.silva@example.com", null),
            address =
                Address(
                    street = "Rua das Flores",
                    number = "123",
                    complement = null,
                    neighborhood = "Centro",
                    city = "Sao Paulo",
                    state = "SP",
                    zipCode = "01001000",
                    country = "BR",
                ),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
}
