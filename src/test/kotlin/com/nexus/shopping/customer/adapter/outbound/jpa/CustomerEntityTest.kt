package com.nexus.shopping.customer.adapter.outbound.jpa

import com.nexus.shopping.customer.application.command.CreateCustomerCommand
import com.nexus.shopping.customer.application.exception.CustomerValidationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CustomerEntityTest {
    @Test
    fun `toEntity throws CustomerValidationException when document type is invalid`() {
        assertFailsWith<CustomerValidationException> {
            validCommand().copy(documentType = "PASSPORT").toEntity()
        }
    }

    private fun validCommand() =
        CreateCustomerCommand(
            name = "Ana Silva",
            document = "02648629025",
            documentType = "CPF",
            email = "ana.silva@example.com",
            phone = "+5511999990000",
            street = "Rua das Flores",
            number = "123",
            complement = null,
            neighborhood = "Centro",
            city = "Sao Paulo",
            state = "SP",
            zipCode = "01001000",
            country = "BR",
        )
}
