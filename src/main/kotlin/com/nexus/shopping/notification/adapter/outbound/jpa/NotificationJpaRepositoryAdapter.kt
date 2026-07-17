package com.nexus.shopping.notification.adapter.outbound.jpa

import com.nexus.shopping.notification.application.exception.NotificationValidationException
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.platform.domain.PageResult
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class NotificationJpaRepositoryAdapter(
    private val repository: SpringDataNotificationRepository,
) : NotificationRepositoryPort {
    @Transactional
    override fun save(notification: Notification): Notification =
        try {
            repository.saveAndFlush(notification.toEntity()).toDomain()
        } catch (exception: DataIntegrityViolationException) {
            val constraintName = (exception.cause as? ConstraintViolationException)?.constraintName
            if (constraintName != null && constraintName.contains("fk_notifications_customer", ignoreCase = true)) {
                throw NotificationValidationException("customerId ${notification.customerId} does not reference an existing customer.")
            }
            throw exception
        }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Notification? = repository.findById(id).orElse(null)?.toDomain()

    @Transactional(readOnly = true)
    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Notification> {
        val slice = repository.findByCustomerId(customerId, PageRequest.of(page, size))
        val content = slice.content.map { it.toDomain() }

        return PageResult(
            content = content,
            page = page,
            size = size,
            count = content.size,
            hasNext = slice.hasNext(),
        )
    }
}
