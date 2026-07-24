package com.nexus.shopping.order.application.exception

import com.nexus.shopping.platform.application.exception.ApplicationException

class OrderIdempotencyConflictException(
    message: String,
) : ApplicationException(message)
