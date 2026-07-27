package com.nexus.shopping.integration.checkout.application.exception

import com.nexus.shopping.platform.application.exception.ValidationException

class CheckoutValidationException(
    message: String,
) : ValidationException(message)
