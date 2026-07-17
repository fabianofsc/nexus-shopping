package com.nexus.shopping.support

import com.nexus.shopping.product.domain.Product
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@ContextConfiguration(classes = [RedisTestConfiguration::class])
abstract class RedisIntegrationTest

@TestConfiguration(proxyBeanMethods = false)
class RedisTestConfiguration {
    @Bean
    @Primary
    fun testProductRedisTemplate(): RedisTemplate<String, Product> {
        val entries = ConcurrentHashMap<String, Product>()
        val valueOperations = valueOperations(entries)
        val redisTemplate = redisTemplate()

        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(redisTemplate.delete(anyString())).thenAnswer { invocation ->
            entries.remove(invocation.getArgument<String>(0)) != null
        }

        return redisTemplate
    }

    @Suppress("UNCHECKED_CAST")
    private fun redisTemplate(): RedisTemplate<String, Product> = mock(RedisTemplate::class.java) as RedisTemplate<String, Product>

    @Suppress("UNCHECKED_CAST")
    private fun valueOperations(entries: ConcurrentHashMap<String, Product>): ValueOperations<String, Product> {
        val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, Product>

        `when`(valueOperations.get(anyString())).thenAnswer { invocation ->
            entries[invocation.getArgument<String>(0)]
        }
        doAnswer { invocation ->
            entries[invocation.getArgument<String>(0)] = invocation.getArgument(1)
            null
        }
            .`when`(valueOperations)
            .set(anyString(), any(Product::class.java), any(Duration::class.java))

        return valueOperations
    }
}
