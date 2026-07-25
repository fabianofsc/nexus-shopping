package com.nexus.shopping.order.domain

data class CustomerSnapshot(
    val customerId: Long,
    val name: String,
    val document: String,
    val documentType: String,
    val email: String,
    val phone: String?,
)
