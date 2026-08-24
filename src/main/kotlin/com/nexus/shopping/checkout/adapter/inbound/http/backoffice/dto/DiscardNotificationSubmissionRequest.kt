package com.nexus.shopping.checkout.adapter.inbound.http.backoffice.dto

import com.nexus.shopping.checkout.application.model.DiscardNotificationSubmissionCommand

data class DiscardNotificationSubmissionRequest(
    val reason: String,
)

fun DiscardNotificationSubmissionRequest.toCommand(submissionId: Long): DiscardNotificationSubmissionCommand =
    DiscardNotificationSubmissionCommand(
        submissionId = submissionId,
        reason = reason,
    )
