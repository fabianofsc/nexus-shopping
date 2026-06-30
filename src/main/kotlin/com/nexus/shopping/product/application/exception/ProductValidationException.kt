package com.nexus.shopping.product.application.exception

import com.nexus.shopping.platform.application.exception.ValidationException

class ProductValidationException(
    message: String,
) : ValidationException(message)
