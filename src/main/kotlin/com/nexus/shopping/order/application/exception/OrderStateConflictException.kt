package com.nexus.shopping.order.application.exception

import com.nexus.shopping.platform.application.exception.ConflictException

class OrderStateConflictException(
    message: String,
) : ConflictException(message)
