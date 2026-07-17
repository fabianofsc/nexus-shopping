package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * Cache-aside (Redis, distributed) decorator around [ProductJpaRepositoryAdapter].
 *
 * Only [findById] is cached. The detail key is shared by all application instances, so writes
 * invalidate the same cache entry used by every instance.
 */
@Primary
@Repository
class CachingProductRepositoryAdapter(
    private val delegate: ProductJpaRepositoryAdapter,
    private val redisTemplate: RedisTemplate<String, Product>,
    private val properties: ProductCacheProperties,
) : ProductRepositoryPort {
    override fun findById(id: Long): Product? {
        val cacheKey = detailCacheKey(id)
        val cached = redisTemplate.opsForValue().get(cacheKey)
        if (cached != null) {
            logger.info("cache HIT id={}", id)
            return cached
        }

        logger.info("cache MISS id={}", id)
        val product = delegate.findById(id)
        if (product != null) {
            redisTemplate.opsForValue().set(cacheKey, product, properties.ttl)
        }
        return product
    }

    override fun findByCategoryId(
        categoryId: Long,
        page: Int,
        size: Int,
    ): ProductPage = delegate.findByCategoryId(categoryId, page, size)

    override fun findByName(
        name: String,
        page: Int,
        size: Int,
    ): ProductPage = delegate.findByName(name, page, size)

    override fun save(command: CreateProductCommand): Product = delegate.save(command)

    override fun updatePrice(
        id: Long,
        priceAmount: BigDecimal,
    ): Product? {
        val updated = delegate.updatePrice(id, priceAmount)
        redisTemplate.delete(detailCacheKey(id))
        return updated
    }

    private fun detailCacheKey(id: Long): String = "products:detail::$id"

    private companion object {
        private val logger = LoggerFactory.getLogger(CachingProductRepositoryAdapter::class.java)
    }
}
