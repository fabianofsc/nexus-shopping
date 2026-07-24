package com.nexus.shopping.order.domain

import java.math.BigDecimal

data class OrderItemSnapshot(
    val productId: Long,
    val productName: String,
    val unitPriceAmount: BigDecimal,
    val currency: Currency,
    val quantity: Int,
) {
    val totalAmount: BigDecimal
        get() = unitPriceAmount.multiply(quantity.toBigDecimal())
}
