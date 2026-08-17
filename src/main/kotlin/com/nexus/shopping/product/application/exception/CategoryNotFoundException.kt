package com.nexus.shopping.product.application.exception

import com.nexus.shopping.platform.application.exception.NotFoundException

class CategoryNotFoundException(
    message: String,
) : NotFoundException(message)
