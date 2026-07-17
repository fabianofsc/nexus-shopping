package com.nexus.shopping.product.adapter.outbound.jpa

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.nexus.shopping.product.domain.Product
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ProductCacheProperties::class)
class ProductCacheConfig {
    @Bean
    fun productCache(properties: ProductCacheProperties): Cache<Long, Product> =
        Caffeine
            .newBuilder()
            .maximumSize(properties.maxSize)
            .expireAfterWrite(properties.ttl)
            .build()
}
