package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.application.usecase.ProductGetByIdUseCase
import com.nexus.shopping.support.RedisIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Proves that, at runtime, Spring resolves the `@Primary` [ProductRepositoryPort] bean to the
 * [CachingProductRepositoryAdapter] decorator, not the plain [ProductJpaRepositoryAdapter]. This
 * is the "definition of done" evidence for the cache-aside feature: without it, nothing actually
 * guarantees the caching decorator sits in front of use cases in the real application context.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:caching_product_repository_wiring_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=1",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class CachingProductRepositoryWiringTest : RedisIntegrationTest() {
    @Autowired
    private lateinit var productRepositoryPort: ProductRepositoryPort

    @Autowired
    private lateinit var productGetByIdUseCase: ProductGetByIdUseCase

    @Test
    fun `primary ProductRepositoryPort bean is the caching decorator`() {
        assertIs<CachingProductRepositoryAdapter>(productRepositoryPort)
    }

    @Test
    fun `ProductGetByIdUseCase is wired with the caching decorator`() {
        val field = ProductGetByIdUseCase::class.java.getDeclaredField("productRepository")
        field.isAccessible = true
        val injectedRepository = field.get(productGetByIdUseCase)

        assertIs<CachingProductRepositoryAdapter>(injectedRepository)
    }
}
