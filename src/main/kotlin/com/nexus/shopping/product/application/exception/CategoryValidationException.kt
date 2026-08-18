package com.nexus.shopping.product.application.exception

import com.nexus.shopping.platform.application.exception.ValidationException

class CategoryValidationException(
    message: String,
) : ValidationException(message)
