package com.nexus.shopping.platform.domain

data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val count: Int,
    val hasNext: Boolean,
)
