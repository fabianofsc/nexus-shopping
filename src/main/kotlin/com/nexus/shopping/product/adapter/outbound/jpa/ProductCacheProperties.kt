package com.nexus.shopping.product.adapter.outbound.jpa

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "nexus.cache.product")
data class ProductCacheProperties(
    val ttl: Duration = Duration.ofMinutes(10),
    val defaultTtl: Duration = Duration.ofMinutes(10),
)
