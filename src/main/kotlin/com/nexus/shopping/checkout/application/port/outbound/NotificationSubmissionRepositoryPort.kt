package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.NotificationSubmission
import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import com.nexus.shopping.platform.domain.PageResult
import java.time.Instant

interface NotificationSubmissionRepositoryPort {
    fun reserve(submission: NotificationSubmission): NotificationSubmission

    fun findById(submissionId: Long): NotificationSubmission?

    fun claim(
        submissionId: Long,
        sendingLeaseToken: String,
        sendingLeaseUntil: Instant,
        now: Instant,
    ): NotificationSubmission?

    fun markAccepted(
        submissionId: Long,
        sendingLeaseToken: String,
        notificationId: String,
        now: Instant,
    ): NotificationSubmission?

    fun markFailed(
        submissionId: Long,
        sendingLeaseToken: String,
        lastError: String,
        now: Instant,
    ): NotificationSubmission?

    fun discard(
        submissionId: Long,
        reason: String,
        now: Instant,
    ): NotificationSubmission?

    fun findPage(
        status: NotificationSubmissionStatus?,
        page: Int,
        size: Int,
    ): PageResult<NotificationSubmission>
}
