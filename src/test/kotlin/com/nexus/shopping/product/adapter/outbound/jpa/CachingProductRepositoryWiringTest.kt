package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.application.usecase.ProductGetByIdUseCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.aop.support.AopUtils
import org.springframework.util.ClassUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves that Spring Cache proxies the actual outbound adapter. The cache behavior must not live
 * in a separate primary decorator, otherwise use cases would bypass the annotated adapter.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:caching_product_repository_wiring_test;DB_CLOSE_DELAY=-1",
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
class CachingProductRepositoryWiringTest {
    @Autowired
    private lateinit var productRepositoryPort: ProductRepositoryPort

    @Autowired
    private lateinit var productGetByIdUseCase: ProductGetByIdUseCase

    @Test
    fun `ProductRepositoryPort is a Spring proxy around the JPA adapter`() {
        assertTrue(AopUtils.isAopProxy(productRepositoryPort))
        assertEquals(ProductJpaRepositoryAdapter::class.java, AopUtils.getTargetClass(productRepositoryPort))
    }

    @Test
    fun `ProductGetByIdUseCase is wired with the proxied JPA adapter`() {
        val field = ProductGetByIdUseCase::class.java.getDeclaredField("productRepository")
        field.isAccessible = true
        val injectedRepository = field.get(productGetByIdUseCase)

        assertTrue(AopUtils.isAopProxy(injectedRepository))
        assertEquals(ProductJpaRepositoryAdapter::class.java, AopUtils.getTargetClass(injectedRepository))
    }

    @Test
    fun `explicit caching decorator is absent from the runtime classpath`() {
        assertFalse(
            ClassUtils.isPresent(
                "com.nexus.shopping.product.adapter.outbound.jpa.CachingProductRepositoryAdapter",
                javaClass.classLoader,
            ),
        )
    }
}
