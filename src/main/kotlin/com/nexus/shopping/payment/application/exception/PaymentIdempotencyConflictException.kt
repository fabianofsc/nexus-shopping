package com.nexus.shopping.payment.application.exception

import com.nexus.shopping.platform.application.exception.ConflictException

class PaymentIdempotencyConflictException(
    message: String,
) : ConflictException(message)
