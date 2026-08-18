package com.nexus.shopping.inventory.adapter.outbound.jpa

import com.nexus.shopping.inventory.application.port.outbound.ProductStockPort
import com.nexus.shopping.inventory.application.port.outbound.StockLedgerPort
import com.nexus.shopping.inventory.domain.StockMovement
import com.nexus.shopping.product.adapter.outbound.jpa.ProductCacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Repository

@Repository
class StockLedgerJpaRepositoryAdapter(
    private val repository: SpringDataStockMovementRepository,
) : StockLedgerPort,
    ProductStockPort {
    override fun record(movement: StockMovement) {
        repository.save(movement.toNewEntity())
    }

    // Stock lives in the products table, so a movement invalidates the catalog read caches the
    // same way a price or details update does in ProductJpaRepositoryAdapter. Without this the
    // release done by the payment reconciliation leaves GET /products/{id} serving stale stock.
    @Caching(
        evict = [
            CacheEvict(cacheNames = [ProductCacheConfig.PRODUCT_DETAIL_CACHE], key = "#productId"),
            CacheEvict(cacheNames = [ProductCacheConfig.PRODUCT_SEARCH_CACHE], allEntries = true),
        ],
    )
    override fun decrementIfAvailable(
        productId: Long,
        quantity: Int,
    ): Boolean = repository.decrementIfAvailable(productId, quantity) == 1

    @Caching(
        evict = [
            CacheEvict(cacheNames = [ProductCacheConfig.PRODUCT_DETAIL_CACHE], key = "#productId"),
            CacheEvict(cacheNames = [ProductCacheConfig.PRODUCT_SEARCH_CACHE], allEntries = true),
        ],
    )
    override fun increment(
        productId: Long,
        quantity: Int,
    ) {
        repository.increment(productId, quantity)
    }
}
