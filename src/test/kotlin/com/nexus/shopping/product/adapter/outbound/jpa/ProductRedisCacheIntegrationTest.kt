package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:product_redis_cache_integration_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=1",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.cache.type=redis",
        "nexus.cache.redis.enabled=true",
        "management.health.redis.enabled=false",
    ],
)
class ProductRedisCacheIntegrationTest {
    @Autowired
    private lateinit var productRepository: ProductRepositoryPort

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @MockitoSpyBean
    private lateinit var springDataRepository: SpringDataProductRepository

    @Test
    fun `production Redis cache manager deserializes product detail on cache hit`() {
        assertIs<RedisCacheManager>(cacheManager)

        val first = assertNotNull(productRepository.findById(1L))

        val cachedValue = stringRedisTemplate.opsForValue().get("products:detail::1")
        assertNotNull(cachedValue)
        assertTrue(cachedValue.startsWith("{") || cachedValue.startsWith("["))

        val cached = assertNotNull(productRepository.findById(1L))

        assertEquals(first, cached)
        verify(springDataRepository, times(1)).findById(1L)
    }

    private companion object {
        @Container
        @JvmField
        val redis =
            GenericContainer<Nothing>("redis:7-alpine").apply {
                withExposedPorts(6379)
            }

        @JvmStatic
        @DynamicPropertySource
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
            registry.add("nexus.cache.redis.enabled") { true }
        }
    }
}
