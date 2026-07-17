package com.nexus.shopping.notification.adapter.outbound.jpa

import com.nexus.shopping.notification.application.exception.NotificationValidationException
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationPage
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
            throw NotificationValidationException("customerId ${notification.customerId} does not reference an existing customer.")
        }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Notification? = repository.findById(id).orElse(null)?.toDomain()

    @Transactional(readOnly = true)
    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): NotificationPage {
        val slice = repository.findByCustomerId(customerId, PageRequest.of(page, size))
        val content = slice.content.map { it.toDomain() }

        return NotificationPage(
            content = content,
            page = page,
            size = size,
            count = content.size,
            hasNext = slice.hasNext(),
        )
    }
}
