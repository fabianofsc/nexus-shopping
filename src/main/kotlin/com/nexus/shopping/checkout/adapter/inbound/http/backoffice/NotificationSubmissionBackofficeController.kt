package com.nexus.shopping.checkout.adapter.inbound.http.backoffice

import com.nexus.shopping.checkout.adapter.inbound.http.backoffice.dto.DiscardNotificationSubmissionRequest
import com.nexus.shopping.checkout.adapter.inbound.http.backoffice.dto.NotificationSubmissionBackofficeResponse
import com.nexus.shopping.checkout.adapter.inbound.http.backoffice.dto.toCommand
import com.nexus.shopping.checkout.adapter.inbound.http.backoffice.dto.toResponse
import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import com.nexus.shopping.checkout.application.port.inbound.NotificationSubmissionBackofficeInputPort
import com.nexus.shopping.platform.adapter.inbound.http.dto.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/backoffice/notification-submissions")
class NotificationSubmissionBackofficeController(
    private val notificationSubmissions: NotificationSubmissionBackofficeInputPort,
) {
    @GetMapping
    fun list(
        @RequestParam(required = false) status: NotificationSubmissionStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): PageResponse<NotificationSubmissionBackofficeResponse> = notificationSubmissions.list(status, page, size).toResponse()

    @PostMapping("/{submissionId}/retry")
    fun retry(
        @PathVariable submissionId: Long,
    ): NotificationSubmissionBackofficeResponse = notificationSubmissions.retry(submissionId).toResponse()

    @PostMapping("/{submissionId}/discard")
    fun discard(
        @PathVariable submissionId: Long,
        @RequestBody request: DiscardNotificationSubmissionRequest,
    ): NotificationSubmissionBackofficeResponse = notificationSubmissions.discard(request.toCommand(submissionId)).toResponse()
}
