package com.nexus.shopping.product.adapter.outbound.jpa

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.nexus.shopping.product.domain.Currency
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductStatus
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    private fun newCache(): Cache<Long, Product> = Caffeine.newBuilder().maximumSize(100).build()

    @Test
    fun `first findById is a MISS and calls the delegate once`() {
        val delegate = CountingProductJpaRepositoryAdapter().apply { findByIdResult = aProduct() }
        val adapter = CachingProductRepositoryAdapter(delegate, newCache())

        val result = adapter.findById(1L)

        assertEquals(aProduct(), result)
        assertEquals(1, delegate.findByIdCalls)
    }

    @Test
    fun `second findById for the same id is a HIT and does not call the delegate again`() {
        val delegate = CountingProductJpaRepositoryAdapter().apply { findByIdResult = aProduct() }
        val adapter = CachingProductRepositoryAdapter(delegate, newCache())

        adapter.findById(1L)
        val result = adapter.findById(1L)

        assertEquals(aProduct(), result)
        assertEquals(1, delegate.findByIdCalls)
    }

    @Test
    fun `updatePrice invalidates the cache so the next findById is a MISS`() {
        val delegate = CountingProductJpaRepositoryAdapter().apply { findByIdResult = aProduct() }
        val adapter = CachingProductRepositoryAdapter(delegate, newCache())
        adapter.findById(1L)

        val updatedProduct = aProduct().copy(priceAmount = BigDecimal("29.90"))
        delegate.updatePriceResult = updatedProduct
        delegate.findByIdResult = updatedProduct
        adapter.updatePrice(1L, BigDecimal("29.90"))
        val result = adapter.findById(1L)

        assertEquals(2, delegate.findByIdCalls)
        assertEquals(1, delegate.updatePriceCalls)
        assertEquals(BigDecimal("29.90"), result?.priceAmount)
    }

    @Test
    fun `findById result is not cached when the product does not exist`() {
        val delegate = CountingProductJpaRepositoryAdapter().apply { findByIdResult = null }
        val adapter = CachingProductRepositoryAdapter(delegate, newCache())

        val first = adapter.findById(1L)
        val second = adapter.findById(1L)

        assertNull(first)
        assertNull(second)
        assertEquals(2, delegate.findByIdCalls)
    }
}
