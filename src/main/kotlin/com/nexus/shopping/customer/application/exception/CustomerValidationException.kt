package com.nexus.shopping.customer.application.exception

import com.nexus.shopping.platform.application.exception.ValidationException

class CustomerValidationException(
    message: String,
) : ValidationException(message)
