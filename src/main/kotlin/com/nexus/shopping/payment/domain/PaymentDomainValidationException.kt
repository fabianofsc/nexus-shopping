package com.nexus.shopping.payment.domain

class PaymentDomainValidationException(
    message: String,
) : IllegalArgumentException(message)
