package com.nexus.shopping.payment.application.exception

import com.nexus.shopping.platform.application.exception.ValidationException

class PaymentValidationException(
    message: String,
) : ValidationException(message)
