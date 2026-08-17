package com.nexus.shopping.inventory.application.exception

import com.nexus.shopping.platform.application.exception.ConflictException

class InsufficientStockException(
    productId: Long,
    requestedQuantity: Int,
) : ConflictException("Insufficient stock for product $productId (requested $requestedQuantity).")
