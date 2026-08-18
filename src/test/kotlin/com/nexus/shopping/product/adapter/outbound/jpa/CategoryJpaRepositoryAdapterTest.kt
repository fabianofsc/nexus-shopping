package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.command.CreateCategoryCommand
import com.nexus.shopping.product.domain.CategoryStatus
import com.nexus.shopping.support.RedisIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:category_jpa_repository_adapter_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@Transactional
class CategoryJpaRepositoryAdapterTest : RedisIntegrationTest() {
    @Autowired
    private lateinit var adapter: CategoryJpaRepositoryAdapter

    @Test
    fun `create persists and findById returns the category`() {
        val created = adapter.save(CreateCategoryCommand(parentId = null, name = "Eletronicos", slug = "eletronicos", status = "ACTIVE"))

        assertNotNull(created.id)
        val found = adapter.findById(requireNotNull(created.id))
        assertEquals("Eletronicos", found?.name)
        assertEquals("eletronicos", found?.slug)
        assertEquals(CategoryStatus.ACTIVE, found?.status)
    }

    @Test
    fun `findById returns null when category does not exist`() {
        assertNull(adapter.findById(999_999_999L))
    }

    @Test
    fun `updateStatus changes the category status`() {
        val created = adapter.save(CreateCategoryCommand(parentId = null, name = "Roupas", slug = "roupas", status = "ACTIVE"))

        val updated = adapter.updateStatus(requireNotNull(created.id), CategoryStatus.INACTIVE)

        assertNotNull(updated)
        assertEquals(CategoryStatus.INACTIVE, updated!!.status)
    }

    @Test
    fun `updateStatus returns null when category does not exist`() {
        assertNull(adapter.updateStatus(999_999_999L, CategoryStatus.INACTIVE))
    }

    @Test
    fun `findAll returns the seeded categories plus any created`() {
        val before = adapter.findAll().size

        adapter.save(CreateCategoryCommand(parentId = null, name = "Livros", slug = "livros", status = "ACTIVE"))

        assertEquals(before + 1, adapter.findAll().size)
    }
}
