package com.nexus.shopping.checkout.adapter.outbound.jpa

import com.nexus.shopping.checkout.application.model.NotificationSubmission
import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import com.nexus.shopping.checkout.application.port.outbound.NotificationSubmissionRepositoryPort
import com.nexus.shopping.platform.domain.PageResult
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
class NotificationSubmissionJpaRepositoryAdapter(
    private val repository: SpringDataNotificationSubmissionRepository,
) : NotificationSubmissionRepositoryPort {
    @Transactional
    override fun reserve(submission: NotificationSubmission): NotificationSubmission {
        val existing = repository.findByNotificationKey(submission.notificationKey).orElse(null)
        if (existing != null) return existing.toDomain()

        return try {
            repository.saveAndFlush(submission.toEntity(Instant.now())).toDomain()
        } catch (exception: DataIntegrityViolationException) {
            repository.findByNotificationKey(submission.notificationKey).orElseThrow { exception }.toDomain()
        }
    }

    @Transactional(readOnly = true)
    override fun findById(submissionId: Long): NotificationSubmission? =
        repository.findSubmissionById(submissionId).orElse(null)?.toDomain()

    @Transactional
    override fun claim(
        submissionId: Long,
        sendingLeaseToken: String,
        sendingLeaseUntil: Instant,
        now: Instant,
    ): NotificationSubmission? =
        updateAndFind(submissionId) {
            repository.claimIfAvailable(submissionId, sendingLeaseToken, sendingLeaseUntil, now)
        }

    @Transactional
    override fun markAccepted(
        submissionId: Long,
        sendingLeaseToken: String,
        notificationId: String,
        now: Instant,
    ): NotificationSubmission? =
        updateAndFind(submissionId) {
            repository.markAcceptedIfCurrentLease(submissionId, sendingLeaseToken, notificationId, now)
        }

    @Transactional
    override fun markFailed(
        submissionId: Long,
        sendingLeaseToken: String,
        lastError: String,
        now: Instant,
    ): NotificationSubmission? =
        updateAndFind(submissionId) {
            repository.markFailedIfCurrentLease(submissionId, sendingLeaseToken, lastError, now)
        }

    @Transactional
    override fun discard(
        submissionId: Long,
        reason: String,
        now: Instant,
    ): NotificationSubmission? =
        updateAndFind(submissionId) {
            repository.discardIfAllowed(submissionId, reason, now)
        }

    @Transactional(readOnly = true)
    override fun findPage(
        status: NotificationSubmissionStatus?,
        page: Int,
        size: Int,
    ): PageResult<NotificationSubmission> {
        val sort = Sort.by("createdAt").ascending().and(Sort.by("id").ascending())
        val pageable = PageRequest.of(page, size, sort)
        val slice = repository.findPage(status, pageable)
        return PageResult(
            content = slice.content.map { it.toDomain() },
            page = page,
            size = size,
            count = slice.numberOfElements,
            hasNext = slice.hasNext(),
        )
    }

    private fun updateAndFind(
        submissionId: Long,
        update: () -> Int,
    ): NotificationSubmission? {
        if (update() != 1) return null
        return repository.findSubmissionById(submissionId).orElseThrow().toDomain()
    }
}
