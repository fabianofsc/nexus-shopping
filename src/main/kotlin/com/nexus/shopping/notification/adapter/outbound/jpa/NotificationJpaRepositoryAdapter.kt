package com.nexus.shopping.notification.adapter.outbound.jpa

import com.nexus.shopping.notification.application.exception.NotificationValidationException
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.platform.domain.PageResult
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

@Repository
class NotificationJpaRepositoryAdapter(
    private val repository: SpringDataNotificationRepository,
    transactionManager: PlatformTransactionManager,
) : NotificationRepositoryPort {
    private val transactions =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    @Transactional
    override fun save(notification: Notification): Notification =
        try {
            repository.saveAndFlush(notification.toEntity()).toDomain()
        } catch (exception: DataIntegrityViolationException) {
            throwMappedConstraintViolation(notification, exception)
        }

    override fun reserve(notification: Notification): Notification =
        try {
            requireNotNull(
                transactions.execute {
                    repository.findByNotificationKey(notification.notificationKey).orElse(null)?.toDomain()
                        ?: repository.saveAndFlush(notification.toEntity()).toDomain()
                },
            )
        } catch (exception: DataIntegrityViolationException) {
            val existing =
                transactions.execute {
                    repository.findByNotificationKey(notification.notificationKey).orElse(null)?.toDomain()
                }
            existing ?: throwMappedConstraintViolation(notification, exception)
        }

    override fun claim(
        notificationKey: String,
        sendingLeaseToken: String,
        sendingLeaseUntil: Instant,
        now: Instant,
    ): Notification? {
        var claimedNotification: Notification? = null
        transactions.executeWithoutResult {
            val claimed =
                repository.claimForSending(
                    notificationKey = notificationKey,
                    sendingLeaseToken = sendingLeaseToken,
                    sendingLeaseUntil = sendingLeaseUntil,
                    now = now,
                    sendingStatus = NotificationStatus.SENDING,
                    claimableStatuses = listOf(NotificationStatus.PENDING, NotificationStatus.FAILED),
                ) == 1
            if (claimed) {
                claimedNotification = repository.findByNotificationKey(notificationKey).orElseThrow().toDomain()
            }
        }
        return claimedNotification
    }

    override fun complete(
        notificationKey: String,
        sendingLeaseToken: String,
        status: NotificationStatus,
        sentAt: Instant?,
    ): Notification? {
        require(status == NotificationStatus.SENT || status == NotificationStatus.FAILED) {
            "Notification completion status must be SENT or FAILED."
        }
        var completedNotification: Notification? = null
        transactions.executeWithoutResult {
            val completed =
                repository.completeIfCurrentLeaseToken(
                    notificationKey = notificationKey,
                    sendingLeaseToken = sendingLeaseToken,
                    sendingStatus = NotificationStatus.SENDING,
                    status = status,
                    sentAt = sentAt,
                ) == 1
            if (completed) {
                completedNotification = repository.findByNotificationKey(notificationKey).orElseThrow().toDomain()
            }
        }
        return completedNotification
    }

    @Transactional(readOnly = true)
    override fun findByNotificationKey(notificationKey: String): Notification? =
        repository.findByNotificationKey(notificationKey).orElse(null)?.toDomain()

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

    private fun throwMappedConstraintViolation(
        notification: Notification,
        exception: DataIntegrityViolationException,
    ): Nothing {
        val constraintName = (exception.cause as? ConstraintViolationException)?.constraintName
        if (constraintName != null && constraintName.contains("fk_notifications_customer", ignoreCase = true)) {
            throw NotificationValidationException("customerId ${notification.customerId} does not reference an existing customer.")
        }
        throw exception
    }
}
