package com.nexus.shopping.order.application.exception

import com.nexus.shopping.platform.application.exception.ValidationException

class OrderValidationException(
    message: String,
) : ValidationException(message)
