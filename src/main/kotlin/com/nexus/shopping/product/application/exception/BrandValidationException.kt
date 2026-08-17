package com.nexus.shopping.product.application.exception

import com.nexus.shopping.platform.application.exception.ValidationException

class BrandValidationException(
    message: String,
) : ValidationException(message)
