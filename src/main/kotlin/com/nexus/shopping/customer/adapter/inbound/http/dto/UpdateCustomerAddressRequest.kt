package com.nexus.shopping.customer.adapter.inbound.http.dto

import com.nexus.shopping.customer.application.command.UpdateCustomerAddressCommand

data class UpdateCustomerAddressRequest(
    val street: String,
    val number: String,
    val complement: String? = null,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String = "BR",
)

fun UpdateCustomerAddressRequest.toCommand(): UpdateCustomerAddressCommand =
    UpdateCustomerAddressCommand(
        street = street,
        number = number,
        complement = complement,
        neighborhood = neighborhood,
        city = city,
        state = state,
        zipCode = zipCode,
        country = country,
    )
