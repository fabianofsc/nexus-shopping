package com.nexus.shopping.notification.application.exception

import com.nexus.shopping.platform.application.exception.ValidationException

class NotificationValidationException(
    message: String,
) : ValidationException(message)
