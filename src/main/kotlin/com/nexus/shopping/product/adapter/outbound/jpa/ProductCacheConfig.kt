package com.nexus.shopping.product.adapter.outbound.jpa

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@EnableCaching
@EnableConfigurationProperties(ProductCacheProperties::class)
@Suppress("DEPRECATION")
class ProductCacheConfig {
    // Fallback only: lets tests/local runs disable Redis (nexus.cache.redis.enabled=false) without
    // dropping @Cacheable support. Mutually exclusive with the Redis-backed manager below — never
    // both active at once, so this is not an L1+L2 multi-tier cache.
    @Bean
    @ConditionalOnProperty(
        prefix = "nexus.cache.redis",
        name = ["enabled"],
        havingValue = "false",
    )
    fun productInMemoryCacheManager(): CacheManager =
        ConcurrentMapCacheManager(
            PRODUCT_DETAIL_CACHE,
            PRODUCT_SEARCH_CACHE,
        )

    @Bean
    @ConditionalOnProperty(
        prefix = "nexus.cache.redis",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun productRedisCacheManager(
        connectionFactory: RedisConnectionFactory,
        objectMapperProvider: ObjectProvider<ObjectMapper>,
        properties: ProductCacheProperties,
    ): CacheManager {
        val objectMapper = objectMapperProvider.getIfAvailable(::fallbackObjectMapper)
        val defaultConfiguration =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(properties.defaultTtl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        GenericJackson2JsonRedisSerializer(cacheObjectMapper(objectMapper)),
                    ),
                ).disableCachingNullValues()

        return RedisCacheManager
            .builder(connectionFactory)
            .transactionAware()
            .cacheDefaults(defaultConfiguration)
            .withInitialCacheConfigurations(
                mapOf(
                    PRODUCT_DETAIL_CACHE to defaultConfiguration.entryTtl(properties.ttl),
                    PRODUCT_SEARCH_CACHE to defaultConfiguration.entryTtl(properties.searchTtl),
                ),
            ).build()
    }

    private fun fallbackObjectMapper(): ObjectMapper =
        JsonMapper
            .builder()
            .addModule(KotlinModule.Builder().build())
            .addModule(JavaTimeModule())
            .build()

    private fun cacheObjectMapper(objectMapper: ObjectMapper): ObjectMapper =
        objectMapper
            .copy()
            .activateDefaultTypingAsProperty(
                BasicPolymorphicTypeValidator
                    .builder()
                    .allowIfSubType("com.nexus.shopping.platform.domain.")
                    .allowIfSubType("com.nexus.shopping.product.domain.")
                    .allowIfSubType("java.math.")
                    .allowIfSubType("java.time.")
                    .allowIfSubType("java.util.")
                    .build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                "@class",
            )

    companion object {
        const val PRODUCT_DETAIL_CACHE = "products:detail"
        const val PRODUCT_SEARCH_CACHE = "products:search"
    }
}
