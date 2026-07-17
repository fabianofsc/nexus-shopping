package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Currency
import com.nexus.shopping.product.domain.ProductStatus
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:product_spring_cache_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=1",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.cache.type=simple",
        "nexus.cache.redis.enabled=false",
        "management.health.redis.enabled=false",
    ],
)
class ProductSpringCacheTest {
    @Autowired
    private lateinit var productRepository: ProductRepositoryPort

    @Autowired
    private lateinit var cacheManager: CacheManager

    @MockitoBean
    private lateinit var springDataRepository: SpringDataProductRepository

    @BeforeTest
    fun resetState() {
        reset(springDataRepository)
        cacheManager.getCache("products:detail")?.clear()
        cacheManager.getCache("products:search")?.clear()
    }

    @Test
    fun `findById caches a product in the Spring cache`() {
        `when`(springDataRepository.findById(1L)).thenReturn(Optional.of(productEntity()))

        val first = productRepository.findById(1L)
        val second = productRepository.findById(1L)

        assertEquals(first, second)
        assertNotNull(cacheManager.getCache("products:detail")?.get(1L))
        verify(springDataRepository, times(1)).findById(1L)
    }

    @Test
    fun `updatePrice evicts detail so the next findById reads the updated product`() {
        val original = productEntity(price = BigDecimal("19.90"))
        val updated = productEntity(price = BigDecimal("88.80"))
        `when`(springDataRepository.findById(1L)).thenReturn(Optional.of(original), Optional.of(updated), Optional.of(updated))
        `when`(springDataRepository.updatePriceById(1L, BigDecimal("88.80"))).thenReturn(1)
        cacheManager.getCache("products:search")?.put("product", "stale result")

        assertEquals(BigDecimal("19.90"), productRepository.findById(1L)?.priceAmount)
        assertEquals(BigDecimal("88.80"), productRepository.updatePrice(1L, BigDecimal("88.80"))?.priceAmount)
        assertEquals(BigDecimal("88.80"), productRepository.findById(1L)?.priceAmount)

        assertNull(cacheManager.getCache("products:search")?.get("product"))
        verify(springDataRepository, times(3)).findById(1L)
    }

    @Test
    fun `save evicts all product search entries`() {
        `when`(springDataRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(ProductEntity::class.java))).thenReturn(productEntity())
        cacheManager.getCache("products:search")?.put("product", "stale result")

        productRepository.save(
            CreateProductCommand(
                brandId = 1L,
                categoryId = 1L,
                sku = "SKU-NEW",
                name = "New product",
                slug = "new-product",
                description = null,
                status = "ACTIVE",
                priceAmount = BigDecimal("19.90"),
                currency = "BRL",
                inventoryQuantity = 1,
            ),
        )

        assertNull(cacheManager.getCache("products:search")?.get("product"))
    }

    @Test
    fun `findById does not cache a missing product`() {
        `when`(springDataRepository.findById(404L)).thenReturn(Optional.empty())

        assertNull(productRepository.findById(404L))
        assertNull(productRepository.findById(404L))

        assertNull(cacheManager.getCache("products:detail")?.get(404L))
        verify(springDataRepository, times(2)).findById(404L)
    }

    private fun productEntity(
        price: BigDecimal = BigDecimal("19.90"),
    ) =
        ProductEntity(
            id = 1L,
            brandId = 1L,
            categoryId = 1L,
            sku = "SKU-1",
            name = "Product 1",
            slug = "product-1",
            status = ProductStatus.ACTIVE,
            priceAmount = price,
            currency = Currency.BRL,
            inventoryQuantity = 1,
            createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        )
}
