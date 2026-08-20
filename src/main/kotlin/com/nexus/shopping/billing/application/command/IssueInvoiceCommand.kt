package com.nexus.shopping.billing.application.command

import java.math.BigDecimal

data class IssueInvoiceCommand(
    val orderId: Long,
    val orderReference: String,
    val customer: InvoiceCustomerSnapshot,
    val shippingAddress: InvoiceShippingAddressSnapshot,
    val items: List<InvoiceItemSnapshot>,
    val totalAmount: BigDecimal,
)

data class InvoiceCustomerSnapshot(
    val name: String,
    val document: String,
    val documentType: String,
)

data class InvoiceShippingAddressSnapshot(
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
)

data class InvoiceItemSnapshot(
    val productId: Long,
    val productName: String,
    val unitPriceAmount: BigDecimal,
    val currency: String,
    val quantity: Int,
)
