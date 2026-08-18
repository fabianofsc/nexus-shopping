package com.nexus.shopping.checkout.application.exception

import com.nexus.shopping.platform.application.exception.ValidationException

class CheckoutValidationException(
    message: String,
) : ValidationException(message)
