package com.nexus.shopping.platform.adapter.inbound.http.dto

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val count: Int,
    val hasNext: Boolean,
)
