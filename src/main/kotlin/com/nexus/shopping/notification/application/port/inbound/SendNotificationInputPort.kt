package com.nexus.shopping.notification.application.port.inbound

import com.nexus.shopping.notification.application.command.SendNotificationCommand
import com.nexus.shopping.notification.domain.Notification

interface SendNotificationInputPort {
    fun send(command: SendNotificationCommand): Notification
}
