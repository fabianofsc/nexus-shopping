package com.nexus.shopping.notification.adapter.inbound.http.dto

import com.nexus.shopping.notification.domain.NotificationPage

data class NotificationPageResponse(
    val content: List<NotificationResponse>,
    val page: Int,
    val size: Int,
    val count: Int,
    val hasNext: Boolean,
)

fun NotificationPage.toResponse(): NotificationPageResponse =
    NotificationPageResponse(
        content = content.map { it.toResponse() },
        page = page,
        size = size,
        count = count,
        hasNext = hasNext,
    )
