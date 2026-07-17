package com.nexus.shopping.customer.adapter.inbound.http.dto

import com.nexus.shopping.customer.application.command.CreateCustomerCommand

data class CreateCustomerRequest(
    val name: String,
    val document: String,
    val email: String,
    val phone: String? = null,
    val street: String,
    val number: String,
    val complement: String? = null,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String = "BR",
)

fun CreateCustomerRequest.toCommand(): CreateCustomerCommand =
    CreateCustomerCommand(
        name = name,
        document = document,
        email = email,
        phone = phone,
        street = street,
        number = number,
        complement = complement,
        neighborhood = neighborhood,
        city = city,
        state = state,
        zipCode = zipCode,
        country = country,
    )
