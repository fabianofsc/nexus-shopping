package com.nexus.shopping.order.domain

enum class OrderStatus {
    WAITING_PAYMENT,
    PAYMENT_PROCESSING,
    PAYMENT_FAILED,
    CONFIRMED,
    CANCELLED,
}
