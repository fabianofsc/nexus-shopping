package com.nexus.shopping.checkout.application.usecase

import com.nexus.shopping.checkout.application.exception.CheckoutValidationException
import com.nexus.shopping.checkout.application.model.DiscardNotificationSubmissionCommand
import com.nexus.shopping.checkout.application.model.EnsureOrderConfirmationCommand
import com.nexus.shopping.checkout.application.model.NotificationSubmission
import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import com.nexus.shopping.checkout.application.model.NotificationSubmissionSummary
import com.nexus.shopping.checkout.application.port.inbound.NotificationSubmissionBackofficeInputPort
import com.nexus.shopping.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.checkout.application.port.outbound.NotificationServiceClientPort
import com.nexus.shopping.checkout.application.port.outbound.NotificationServiceRejectedException
import com.nexus.shopping.checkout.application.port.outbound.NotificationServiceUnavailableException
import com.nexus.shopping.checkout.application.port.outbound.NotificationSubmissionRepositoryPort
import com.nexus.shopping.platform.domain.PageResult
import java.time.Instant
import java.util.UUID

class NotificationSubmissionUseCase(
    private val repository: NotificationSubmissionRepositoryPort,
    private val client: NotificationServiceClientPort,
) :
        NotificationGateway,
        NotificationSubmissionBackofficeInputPort {
    override fun reserveOrderConfirmation(command: EnsureOrderConfirmationCommand): NotificationSubmission =
        repository.reserve(NotificationSubmission.forOrderConfirmation(command))

    override fun dispatch(submissionId: Long): NotificationSubmission {
        val current = repository.findById(submissionId) ?: throw submissionNotFound(submissionId)
        val leaseToken = UUID.randomUUID().toString()
        val now = Instant.now()
        val claimed =
            repository.claim(submissionId, leaseToken, now.plusSeconds(30), now)
                ?: return repository.findById(submissionId) ?: current

        return try {
            val accepted = client.accept(claimed)
            repository.markAccepted(submissionId, leaseToken, accepted.notificationId, Instant.now())
                ?: currentSubmission(submissionId, claimed)
        } catch (_: NotificationServiceUnavailableException) {
            repository.markFailed(submissionId, leaseToken, UNAVAILABLE_ERROR, Instant.now())
                ?: currentSubmission(submissionId, claimed)
        } catch (_: NotificationServiceRejectedException) {
            repository.markFailed(submissionId, leaseToken, REJECTED_ERROR, Instant.now())
                ?: currentSubmission(submissionId, claimed)
        }
    }

    override fun ensureOrderConfirmation(command: EnsureOrderConfirmationCommand) {
        dispatch(requireNotNull(reserveOrderConfirmation(command).id))
    }

    override fun list(status: NotificationSubmissionStatus?, page: Int, size: Int): PageResult<NotificationSubmissionSummary> =
        repository.findPage(status, page, size).toSummaryPage()

    override fun retry(submissionId: Long): NotificationSubmissionSummary {
        val submission = repository.findById(submissionId) ?: throw submissionNotFound(submissionId)
        if (!submission.isRetryable()) {
            throw CheckoutValidationException("notification submission $submissionId cannot be retried.")
        }
        return dispatch(submissionId).toSummary()
    }

    override fun discard(command: DiscardNotificationSubmissionCommand): NotificationSubmissionSummary {
        val submission = repository.findById(command.submissionId) ?: throw submissionNotFound(command.submissionId)
        submission.discard(command.reason)
        return repository.discard(command.submissionId, command.reason, Instant.now())?.toSummary()
            ?: throw CheckoutValidationException("notification submission ${command.submissionId} cannot be discarded.")
    }

    private fun currentSubmission(
        submissionId: Long,
        claimed: NotificationSubmission,
    ): NotificationSubmission = repository.findById(submissionId) ?: claimed

    private fun submissionNotFound(submissionId: Long): CheckoutValidationException =
        CheckoutValidationException("notification submission $submissionId was not found.")

    private fun NotificationSubmission.toSummary(): NotificationSubmissionSummary =
        NotificationSubmissionSummary(
            id = requireNotNull(id),
            status = status,
            attemptCount = attemptCount,
            lastError = lastError,
            notificationId = notificationId,
            discardReason = discardReason,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun NotificationSubmission.isRetryable(): Boolean =
        status in setOf(NotificationSubmissionStatus.PENDING, NotificationSubmissionStatus.FAILED) ||
            (status == NotificationSubmissionStatus.IN_FLIGHT && sendingLeaseUntil?.isBefore(Instant.now()) == true)

    private fun PageResult<NotificationSubmission>.toSummaryPage(): PageResult<NotificationSubmissionSummary> =
        PageResult(content.map { it.toSummary() }, page, size, count, hasNext)

    private companion object {
        const val UNAVAILABLE_ERROR = "Notification service is unavailable."
        const val REJECTED_ERROR = "Notification service rejected the submission."
    }
}
