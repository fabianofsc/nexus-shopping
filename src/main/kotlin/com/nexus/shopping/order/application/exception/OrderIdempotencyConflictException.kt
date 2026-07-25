package com.nexus.shopping.order.application.exception

import com.nexus.shopping.platform.application.exception.ConflictException

class OrderIdempotencyConflictException(
    message: String,
) : ConflictException(message)
