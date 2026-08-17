package com.nexus.shopping.inventory.application.exception

import com.nexus.shopping.platform.application.exception.ValidationException

class InventoryValidationException(
    message: String,
) : ValidationException(message)
