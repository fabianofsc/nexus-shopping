package com.nexus.shopping.customer.application.exception

import com.nexus.shopping.platform.application.exception.NotFoundException

class CustomerNotFoundException(
    message: String,
) : NotFoundException(message)
