package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.domain.Currency
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductStatus
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [SpringDataProductRepository] is a Spring Data JPA interface with many inherited abstract
 * members. [CountingProductJpaRepositoryAdapter] below overrides every method the decorator
 * calls, so this instance is only needed to satisfy [ProductJpaRepositoryAdapter]'s constructor
 * and is never actually invoked.
 */
private fun unusedSpringDataRepository(): SpringDataProductRepository {
    val handler =
        InvocationHandler { _, _, _ ->
            throw UnsupportedOperationException("delegate repository should not be used directly in this test")
        }
    return Proxy.newProxyInstance(
        SpringDataProductRepository::class.java.classLoader,
        arrayOf(SpringDataProductRepository::class.java),
        handler,
    ) as SpringDataProductRepository
}

/** Fake delegate: the concrete [ProductJpaRepositoryAdapter] with call counters, no real DB. */
private class CountingProductJpaRepositoryAdapter : ProductJpaRepositoryAdapter(unusedSpringDataRepository()) {
    var findByIdCalls = 0
    var updatePriceCalls = 0
    var findByIdResult: Product? = null
    var updatePriceResult: Product? = null

    override fun findById(id: Long): Product? {
        findByIdCalls++
        return findByIdResult
    }

    override fun updatePrice(
        id: Long,
        priceAmount: BigDecimal,
    ): Product? {
        updatePriceCalls++
        return updatePriceResult
    }
}

@Suppress("DEPRECATION", "UNCHECKED_CAST")
class CachingProductRepositoryAdapterTest {
    private fun aProduct(id: Long = 1L) =
        Product(
            id = id,
            brandId = 1L,
            categoryId = 1L,
            sku = "SKU-$id",
            name = "Product $id",
            slug = "product-$id",
            description = null,
            status = ProductStatus.ACTIVE,
            priceAmount = BigDecimal("19.90"),
            currency = Currency.BRL,
            inventoryQuantity = 0,
            createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        )

    private fun redisTemplate(): RedisTemplate<String, Product> = mock(RedisTemplate::class.java) as RedisTemplate<String, Product>

    @Test
    fun `findById returns cached product from Redis without hitting delegate`() {
        val delegate = CountingProductJpaRepositoryAdapter()
        val redisTemplate = redisTemplate()
        val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, Product>
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.get("products:detail::1")).thenReturn(aProduct())
        val adapter = CachingProductRepositoryAdapter(delegate, redisTemplate, ProductCacheProperties(ttl = Duration.ofMinutes(10)))

        val result = adapter.findById(1L)

        assertEquals(aProduct(), result)
        assertEquals(0, delegate.findByIdCalls)
    }

    @Test
    fun `findById populates Redis with configured TTL after delegate miss`() {
        val delegate = CountingProductJpaRepositoryAdapter().apply { findByIdResult = aProduct() }
        val redisTemplate = redisTemplate()
        val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, Product>
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.get("products:detail::1")).thenReturn(null)
        val ttl = Duration.ofMinutes(10)
        val adapter = CachingProductRepositoryAdapter(delegate, redisTemplate, ProductCacheProperties(ttl = ttl))

        val result = adapter.findById(1L)

        assertEquals(aProduct(), result)
        assertEquals(1, delegate.findByIdCalls)
        verify(valueOperations).set("products:detail::1", aProduct(), ttl)
    }

    @Test
    fun `findById does not cache null delegate result`() {
        val delegate = CountingProductJpaRepositoryAdapter().apply { findByIdResult = null }
        val redisTemplate = redisTemplate()
        val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, Product>
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.get("products:detail::1")).thenReturn(null)
        val adapter = CachingProductRepositoryAdapter(delegate, redisTemplate, ProductCacheProperties())

        val result = adapter.findById(1L)

        assertNull(result)
        assertEquals(1, delegate.findByIdCalls)
        verify(valueOperations).get("products:detail::1")
        verifyNoMoreInteractions(valueOperations)
    }

    @Test
    fun `updatePrice deletes Redis detail cache entry`() {
        val delegate = CountingProductJpaRepositoryAdapter()
        val redisTemplate = redisTemplate()
        val adapter = CachingProductRepositoryAdapter(delegate, redisTemplate, ProductCacheProperties())

        val updatedProduct = aProduct().copy(priceAmount = BigDecimal("29.90"))
        delegate.updatePriceResult = updatedProduct
        val result = adapter.updatePrice(1L, BigDecimal("29.90"))

        assertEquals(updatedProduct, result)
        assertEquals(1, delegate.updatePriceCalls)
        verify(redisTemplate).delete("products:detail::1")
    }

    @Test
    fun `product Redis template serializes keys as strings and values as JSON`() {
        val connectionFactory = mock(RedisConnectionFactory::class.java)
        val redisTemplate = ProductCacheConfig().productRedisTemplate(connectionFactory)

        assertTrue(redisTemplate.keySerializer is StringRedisSerializer)
        assertTrue(redisTemplate.hashKeySerializer is StringRedisSerializer)
        assertTrue(redisTemplate.valueSerializer is GenericJackson2JsonRedisSerializer)
        assertTrue(redisTemplate.hashValueSerializer is GenericJackson2JsonRedisSerializer)
        val valueSerializer = redisTemplate.valueSerializer as RedisSerializer<Any>
        val serialized = valueSerializer.serialize(aProduct())

        assertEquals(aProduct(), valueSerializer.deserialize(serialized))
    }
}
