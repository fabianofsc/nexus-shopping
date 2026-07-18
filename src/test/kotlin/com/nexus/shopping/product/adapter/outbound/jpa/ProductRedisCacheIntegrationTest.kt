package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.test.BeforeTest
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

    @BeforeTest
    fun resetState() {
        reset(springDataRepository)
        assertNotNull(stringRedisTemplate.connectionFactory)
            .connection
            .use { connection -> connection.serverCommands().flushDb() }
    }

    @Test
    fun `production Redis cache manager stores product detail with string key and JSON value`() {
        val redisCacheManager = assertIs<RedisCacheManager>(cacheManager)
        assertTrue(redisCacheManager.isTransactionAware)

        val first = assertNotNull(productRepository.findById(1L))

        val cachedKey = cacheKey(ProductCacheConfig.PRODUCT_DETAIL_CACHE)
        val cachedValue = stringRedisTemplate.opsForValue().get(cachedKey)
        assertNotNull(cachedValue)
        assertTrue(cachedValue.startsWith("{"))
        assertTrue(cachedValue.contains("\"name\""))
        assertTrue(cachedValue.contains("\"priceAmount\""))

        val cached = assertNotNull(productRepository.findById(1L))

        assertEquals(first, cached)
        verify(springDataRepository, times(1)).findById(1L)
    }

    @Test
    fun `production Redis cache manager stores search JSON with a short finite TTL`() {
        productRepository.findById(1L)
        val first = productRepository.findByName(name = "Product 1", page = 0, size = 3)

        val detailKey = cacheKey(ProductCacheConfig.PRODUCT_DETAIL_CACHE)
        val searchKey = cacheKey(ProductCacheConfig.PRODUCT_SEARCH_CACHE)
        val searchValue = assertNotNull(stringRedisTemplate.opsForValue().get(searchKey))
        val detailTtl = pttl(detailKey)
        val searchTtl = pttl(searchKey)

        assertTrue(searchValue.startsWith("{"))
        assertTrue(searchValue.contains("\"name\""))
        assertTrue(searchValue.contains("\"priceAmount\""))
        assertTrue(detailTtl > searchTtl)
        assertTrue(searchTtl in 25_000..30_000, "PTTL de busca deveria ficar proximo de 30s, mas foi $searchTtl ms")

        val second = productRepository.findByName(name = "Product 1", page = 0, size = 3)

        assertEquals(first, second)
        verify(springDataRepository, times(1)).findByNamePrefix(
            "Product 1",
            "Product 2",
            "Product 1%",
            PageRequest.of(0, 3),
        )
    }

    private fun cacheKey(cacheName: String): String {
        val deadline = System.nanoTime() + 2_000_000_000L
        do {
            val key = stringRedisTemplate.keys("$cacheName::*").singleOrNull()
            if (key != null) return key
            Thread.sleep(25)
        } while (System.nanoTime() < deadline)

        return assertNotNull(stringRedisTemplate.keys("$cacheName::*").singleOrNull())
    }

    private fun pttl(key: String): Long =
        assertNotNull(
            stringRedisTemplate.execute { connection ->
                connection.keyCommands().pTtl(key.toByteArray(UTF_8))
            },
        )

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
