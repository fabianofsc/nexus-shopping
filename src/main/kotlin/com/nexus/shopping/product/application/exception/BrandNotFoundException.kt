package com.nexus.shopping.product.application.exception

import com.nexus.shopping.platform.application.exception.NotFoundException

class BrandNotFoundException(
    message: String,
) : NotFoundException(message)
