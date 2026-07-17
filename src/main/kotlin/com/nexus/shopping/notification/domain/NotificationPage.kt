package com.nexus.shopping.notification.domain

data class NotificationPage(
    val content: List<Notification>,
    val page: Int,
    val size: Int,
    val count: Int,
    val hasNext: Boolean,
)
