package com.nexus.shopping.product.adapter.outbound.jpa

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.nexus.shopping.product.domain.Product
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@EnableConfigurationProperties(ProductCacheProperties::class)
@Suppress("DEPRECATION")
class ProductCacheConfig {
    @Bean
    fun productRedisTemplate(
        connectionFactory: RedisConnectionFactory,
    ): RedisTemplate<String, Product> {
        val keySerializer = StringRedisSerializer()
        val objectMapper =
            JsonMapper
                .builder()
                .addModule(KotlinModule.Builder().build())
                .addModule(JavaTimeModule())
                .build()
                .activateDefaultTypingAsProperty(
                    BasicPolymorphicTypeValidator
                        .builder()
                        .allowIfSubType("com.nexus.shopping.product.domain.")
                        .allowIfSubType("java.math.")
                        .allowIfSubType("java.time.")
                        .build(),
                    ObjectMapper.DefaultTyping.EVERYTHING,
                    "@class",
                )
        val valueSerializer = GenericJackson2JsonRedisSerializer(objectMapper)

        return RedisTemplate<String, Product>().apply {
            this.connectionFactory = connectionFactory
            this.keySerializer = keySerializer
            this.hashKeySerializer = keySerializer
            this.valueSerializer = valueSerializer
            this.hashValueSerializer = valueSerializer
            afterPropertiesSet()
        }
    }
}
