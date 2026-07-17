package com.nexus.shopping.notification.application.usecase

import com.nexus.shopping.notification.application.exception.NotificationNotFoundException
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.domain.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GetNotificationByIdUseCase(
    private val notificationRepository: NotificationRepositoryPort,
) {
    fun execute(id: Long): Notification {
        logger.infoWithContext("notification.get_by_id.started", "notification.id" to id)

        val notification = notificationRepository.findById(id)
        if (notification == null) {
            logger.warnWithContext("notification.get_by_id.not_found", "notification.id" to id)
            throw NotificationNotFoundException("Notification $id not found.")
        }

        logger.infoWithContext("notification.get_by_id.completed", "notification.id" to notification.id)
        return notification
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(GetNotificationByIdUseCase::class.java)
    }
}
