package com.nexus.shopping.notification.application.exception

import com.nexus.shopping.platform.application.exception.NotFoundException

class NotificationNotFoundException(
    message: String,
) : NotFoundException(message)
