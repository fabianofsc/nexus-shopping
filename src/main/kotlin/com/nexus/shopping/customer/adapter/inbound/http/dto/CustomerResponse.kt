package com.nexus.shopping.customer.adapter.inbound.http.dto

import com.nexus.shopping.customer.domain.Address
import com.nexus.shopping.customer.domain.Contact
import com.nexus.shopping.customer.domain.Customer
import java.time.LocalDateTime

data class CustomerResponse(
    val id: Long,
    val name: String,
    val document: String,
    val documentType: String,
    val status: String,
    val contact: ContactResponse,
    val address: AddressResponse,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class ContactResponse(
    val email: String,
    val phone: String?,
)

data class AddressResponse(
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
)

fun Customer.toResponse(): CustomerResponse =
    CustomerResponse(
        id = id,
        name = name,
        document = document,
        documentType = documentType.name,
        status = status.name,
        contact = contact.toResponse(),
        address = address.toResponse(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun Contact.toResponse(): ContactResponse =
    ContactResponse(
        email = email,
        phone = phone,
    )

private fun Address.toResponse(): AddressResponse =
    AddressResponse(
        street = street,
        number = number,
        complement = complement,
        neighborhood = neighborhood,
        city = city,
        state = state,
        zipCode = zipCode,
        country = country,
    )
