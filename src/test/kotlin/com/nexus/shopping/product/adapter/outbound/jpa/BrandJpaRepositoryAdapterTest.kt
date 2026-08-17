package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.command.CreateBrandCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:brand_jpa_repository_adapter_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@Transactional
class BrandJpaRepositoryAdapterTest {
    @Autowired
    private lateinit var adapter: BrandJpaRepositoryAdapter

    @Test
    fun `create persists and findById returns the brand`() {
        val created = adapter.save(CreateBrandCommand("Apple", "Tech brand"))

        assertNotNull(created.id)
        val found = adapter.findById(requireNotNull(created.id))
        assertEquals("Apple", found?.name)
        assertEquals("Tech brand", found?.description)
    }

    @Test
    fun `findById returns null when brand does not exist`() {
        assertNull(adapter.findById(999_999_999L))
    }

    @Test
    fun `findAll returns the seeded brands plus any created`() {
        val before = adapter.findAll().size

        adapter.save(CreateBrandCommand("Nike", null))

        assertEquals(before + 1, adapter.findAll().size)
    }
}
