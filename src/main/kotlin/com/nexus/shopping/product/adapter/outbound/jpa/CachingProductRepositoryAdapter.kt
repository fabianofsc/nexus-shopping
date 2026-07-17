package com.nexus.shopping.product.adapter.outbound.jpa

import com.github.benmanes.caffeine.cache.Cache
import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * Cache-aside (LOCAL, Caffeine, in-process) decorator around [ProductJpaRepositoryAdapter].
 *
 * Only [findById] is cached. Cache invalidation on write is local to this instance only:
 * with multiple application instances each keeps its own cache, so a write on one instance
 * does not invalidate the cache on the others. That staleness window is intentional for this
 * teaching fixture and must not be solved here (no shared/distributed cache).
 */
@Primary
@Repository
class CachingProductRepositoryAdapter(
    private val delegate: ProductJpaRepositoryAdapter,
    private val cache: Cache<Long, Product>,
) : ProductRepositoryPort {
    override fun findById(id: Long): Product? {
        val cached = cache.getIfPresent(id)
        if (cached != null) {
            logger.info("cache HIT id={}", id)
            return cached
        }

        logger.info("cache MISS id={}", id)
        val product = delegate.findById(id)
        if (product != null) {
            cache.put(id, product)
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
        cache.invalidate(id)
        return updated
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(CachingProductRepositoryAdapter::class.java)
    }
}
