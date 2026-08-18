package com.nexus.shopping.inventory.adapter.outbound.jpa

import com.nexus.shopping.inventory.domain.StockMovement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:inventory_jpa_repository_adapter_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@Transactional
class StockLedgerJpaRepositoryAdapterTest {
    @Autowired
    private lateinit var adapter: StockLedgerJpaRepositoryAdapter

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `decrementIfAvailable reduces available stock atomically`() {
        setStock(1L, 5)

        assertTrue(adapter.decrementIfAvailable(1L, 3))

        assertEquals(2, stockOf(1L))
    }

    @Test
    fun `decrementIfAvailable returns false when stock is insufficient and changes nothing`() {
        setStock(1L, 2)

        assertFalse(adapter.decrementIfAvailable(1L, 3))

        assertEquals(2, stockOf(1L))
    }

    @Test
    fun `decrementIfAvailable returns false when the product does not exist`() {
        assertFalse(adapter.decrementIfAvailable(999_999L, 1))
    }

    @Test
    fun `record persists a stock movement`() {
        adapter.record(
            StockMovement.decrement(
                productId = 1L,
                quantity = 2,
                reference = "order-1",
                createdAt = LocalDateTime.of(2026, 8, 17, 10, 0),
            ),
        )

        val row = jdbcTemplate.queryForMap("SELECT product_id, quantity, movement_type, reference FROM stock_movements")
        assertEquals(1L, row["product_id"])
        assertEquals(2, row["quantity"])
        assertEquals("DECREASE", row["movement_type"])
        assertEquals("order-1", row["reference"])
    }

    private fun setStock(
        productId: Long,
        quantity: Int,
    ) {
        jdbcTemplate.update("UPDATE products SET inventory_quantity = ? WHERE id = ?", quantity, productId)
    }

    private fun stockOf(productId: Long): Int =
        requireNotNull(
            jdbcTemplate.queryForObject(
                "SELECT inventory_quantity FROM products WHERE id = ?",
                Int::class.java,
                productId,
            ),
        )
}
