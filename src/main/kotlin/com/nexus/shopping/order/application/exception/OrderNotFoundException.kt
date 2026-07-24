package com.nexus.shopping.order.application.exception

import com.nexus.shopping.platform.application.exception.NotFoundException

class OrderNotFoundException(
    message: String,
) : NotFoundException(message)
