package com.nexus.shopping.customer.application.command

data class CreateCustomerCommand(
    val name: String,
    val document: String,
    val email: String,
    val phone: String?,
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
)
