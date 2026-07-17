package com.nexus.shopping.customer.domain

import java.time.LocalDateTime

data class Customer(
    val id: Long,
    val name: String,
    val document: String,
    val documentType: DocumentType,
    val status: CustomerStatus,
    val contact: Contact,
    val address: Address,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
