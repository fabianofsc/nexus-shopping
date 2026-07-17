package com.nexus.shopping.notification.application.usecase

import com.nexus.shopping.notification.application.exception.NotificationValidationException
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import com.nexus.shopping.platform.domain.PageResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ListNotificationsByCustomerUseCase(
    private val notificationRepository: NotificationRepositoryPort,
) {
    fun list(
        customerId: Long?,
        page: Int,
        size: Int,
    ): PageResult<Notification> {
        logger.infoWithContext(
            "notification.list_by_customer.started",
            "notification.customer_id" to customerId,
            "notification.page" to page,
            "notification.size" to size,
        )

        if (customerId == null) throwValidationFailed("Query parameter customerId is required.")
        if (customerId <= 0) throwValidationFailed("Query parameter customerId must be greater than 0.")
        if (page < 0) throwValidationFailed("Query parameter page must be greater than or equal to 0.")
        if (size !in 1..500) throwValidationFailed("Query parameter size must be between 1 and 500.")

        val result = notificationRepository.findByCustomerId(customerId, page, size)

        logger.infoWithContext(
            "notification.list_by_customer.completed",
            "notification.customer_id" to customerId,
            "notification.count" to result.count,
            "notification.has_next" to result.hasNext,
        )
        return result
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("notification.list_by_customer.validation_failed", "validation.error" to message)
        throw NotificationValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ListNotificationsByCustomerUseCase::class.java)
    }
}
