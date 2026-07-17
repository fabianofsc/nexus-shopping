package com.nexus.shopping.notification.adapter.inbound.http

import com.nexus.shopping.notification.adapter.inbound.http.dto.NotificationPageResponse
import com.nexus.shopping.notification.adapter.inbound.http.dto.NotificationResponse
import com.nexus.shopping.notification.adapter.inbound.http.dto.SendNotificationRequest
import com.nexus.shopping.notification.adapter.inbound.http.dto.toCommand
import com.nexus.shopping.notification.adapter.inbound.http.dto.toResponse
import com.nexus.shopping.notification.application.usecase.GetNotificationByIdUseCase
import com.nexus.shopping.notification.application.usecase.ListNotificationsByCustomerUseCase
import com.nexus.shopping.notification.application.usecase.SendNotificationUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val sendNotificationUseCase: SendNotificationUseCase,
    private val getNotificationByIdUseCase: GetNotificationByIdUseCase,
    private val listNotificationsByCustomerUseCase: ListNotificationsByCustomerUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun send(
        @RequestBody request: SendNotificationRequest,
    ): NotificationResponse = sendNotificationUseCase.send(request.toCommand()).toResponse()

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
    ): NotificationResponse = getNotificationByIdUseCase.execute(id).toResponse()

    @GetMapping
    fun listByCustomer(
        @RequestParam(required = false) customerId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): NotificationPageResponse = listNotificationsByCustomerUseCase.list(customerId, page, size).toResponse()
}
