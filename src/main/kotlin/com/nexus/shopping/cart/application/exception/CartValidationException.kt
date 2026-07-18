package com.nexus.shopping.cart.application.exception

import com.nexus.shopping.platform.application.exception.ValidationException

class CartValidationException(
    message: String,
) : ValidationException(message)
