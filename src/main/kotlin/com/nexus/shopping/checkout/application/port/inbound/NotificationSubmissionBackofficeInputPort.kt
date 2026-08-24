package com.nexus.shopping.checkout.application.port.inbound

import com.nexus.shopping.checkout.application.model.DiscardNotificationSubmissionCommand
import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import com.nexus.shopping.checkout.application.model.NotificationSubmissionSummary
import com.nexus.shopping.platform.domain.PageResult

interface NotificationSubmissionBackofficeInputPort {
    fun list(
        status: NotificationSubmissionStatus?,
        page: Int,
        size: Int,
    ): PageResult<NotificationSubmissionSummary>

    fun retry(submissionId: Long): NotificationSubmissionSummary

    fun discard(command: DiscardNotificationSubmissionCommand): NotificationSubmissionSummary
}
