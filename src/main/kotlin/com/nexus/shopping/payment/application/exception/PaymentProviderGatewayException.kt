package com.nexus.shopping.payment.application.exception

import com.nexus.shopping.platform.application.exception.ApplicationException

class PaymentProviderGatewayException(
    message: String,
    cause: Throwable? = null,
) : ApplicationException(message) {
    init {
        cause?.let { initCause(it) }
    }
}
