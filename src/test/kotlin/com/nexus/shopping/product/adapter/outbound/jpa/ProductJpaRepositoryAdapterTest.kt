package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.usecase.CreateProductCommand
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:product_jpa_repository_adapter_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=600",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class ProductJpaRepositoryAdapterTest {

    @Autowired
    private lateinit var repository: ProductJpaRepositoryAdapter

    @Test
    fun `findByCategoryId returns a slice with hasNext without count query`() {
        val result = repository.findByCategoryId(categoryId = 1L, page = 0, size = 1)

        assertEquals(0, result.page)
        assertEquals(1, result.size)
        assertEquals(1, result.count)
        assertTrue(result.hasNext)
        assertEquals(1L, result.content.single().categoryId)
        assertEquals(1L, result.content.single().id)
    }

    @Test
    fun `findByCategoryId returns second slice without hasNext`() {
        val result = repository.findByCategoryId(categoryId = 1L, page = 1, size = 1)

        assertEquals(1, result.page)
        assertEquals(1, result.size)
        assertEquals(1, result.count)
        assertFalse(result.hasNext)
        assertEquals(1L, result.content.single().categoryId)
        assertEquals(6L, result.content.single().id)
    }

    @Test
    fun `findByName uses prefix range bounds`() {
        val result = repository.findByName(name = "Product 1", page = 0, size = 3)

        assertEquals(0, result.page)
        assertEquals(3, result.size)
        assertEquals(3, result.count)
        assertTrue(result.hasNext)
        assertTrue(result.content.all { it.name.startsWith("Product 1") })
        assertEquals(listOf("Product 1", "Product 10", "Product 100"), result.content.map { it.name })
    }

    @Test
    fun `save persists product and returns generated values`() {
        val created = repository.save(
            CreateProductCommand(
                brandId = 1L,
                categoryId = 1L,
                sku = "SKU-JPA-001",
                name = "JPA Adapter Product",
                slug = "jpa-adapter-product",
                description = "Created by the JPA adapter test.",
                status = "ACTIVE",
                priceAmount = BigDecimal("49.90"),
                currency = "BRL",
                inventoryQuantity = 7,
            ),
        )

        assertTrue(created.id > 0)
        assertEquals("SKU-JPA-001", created.sku)
        assertEquals("JPA Adapter Product", created.name)
        assertEquals(0, BigDecimal("49.90").compareTo(created.priceAmount))
        assertNotNull(created.createdAt)
        assertNotNull(created.updatedAt)
    }

    @Test
    fun `updatePrice updates existing product`() {
        val updated = repository.updatePrice(id = 1L, priceAmount = BigDecimal("88.80"))

        assertNotNull(updated)
        assertEquals(1L, updated.id)
        assertEquals(0, BigDecimal("88.80").compareTo(updated.priceAmount))
    }

    @Test
    fun `updatePrice returns null when product does not exist`() {
        val updated = repository.updatePrice(id = 999999999L, priceAmount = BigDecimal("88.80"))

        assertNull(updated)
    }
}
