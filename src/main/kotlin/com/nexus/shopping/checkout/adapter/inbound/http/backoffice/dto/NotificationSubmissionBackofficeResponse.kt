package com.nexus.shopping.checkout.adapter.inbound.http.backoffice.dto

import com.nexus.shopping.checkout.application.model.NotificationSubmissionSummary
import com.nexus.shopping.platform.adapter.inbound.http.dto.PageResponse
import com.nexus.shopping.platform.domain.PageResult
import java.time.Instant

data class NotificationSubmissionBackofficeResponse(
    val id: Long,
    val status: String,
    val attemptCount: Int,
    val lastError: String?,
    val notificationId: String?,
    val discardReason: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun NotificationSubmissionSummary.toResponse(): NotificationSubmissionBackofficeResponse =
    NotificationSubmissionBackofficeResponse(
        id = id,
        status = status.name,
        attemptCount = attemptCount,
        lastError = lastError,
        notificationId = notificationId,
        discardReason = discardReason,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun PageResult<NotificationSubmissionSummary>.toResponse(): PageResponse<NotificationSubmissionBackofficeResponse> =
    PageResponse(
        content = content.map { it.toResponse() },
        page = page,
        size = size,
        count = count,
        hasNext = hasNext,
    )
