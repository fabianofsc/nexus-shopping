package com.nexus.shopping.inventory

import com.nexus.shopping.inventory.application.command.DecrementStockCommand
import com.nexus.shopping.inventory.application.command.DecrementStockItem
import com.nexus.shopping.inventory.application.exception.InsufficientStockException
import com.nexus.shopping.inventory.application.usecase.DecrementStockUseCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves the anti-overselling invariant under real concurrency: two threads racing to
 * decrement the last available unit of the same product must let exactly one succeed,
 * the stock must never go negative, and the ledger must record exactly one DECREASE.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:inventory_stock_concurrency_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class InventoryStockConcurrencyTest {
    @Autowired
    private lateinit var useCase: DecrementStockUseCase

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `concurrent decrements of the last unit let exactly one win and never go negative`() {
        jdbcTemplate.update("UPDATE products SET inventory_quantity = 1 WHERE id = 1")

        val threadCount = 2
        val executor = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val outcomes = ConcurrentLinkedQueue<String>()

        try {
            repeat(threadCount) { index ->
                executor.submit {
                    readyLatch.countDown()
                    startLatch.await()
                    try {
                        useCase.decrement(
                            DecrementStockCommand(
                                orderReference = "order-$index",
                                items = listOf(DecrementStockItem(1, 1)),
                            ),
                        )
                        outcomes += "ok"
                    } catch (exception: InsufficientStockException) {
                        outcomes += "insufficient"
                    }
                    doneLatch.countDown()
                }
            }

            assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "Timed out waiting for worker threads to start")
            startLatch.countDown()
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Timed out waiting for concurrent decrements to finish")
        } finally {
            executor.shutdown()
        }

        assertEquals(listOf("insufficient", "ok"), outcomes.sorted())

        val finalStock =
            jdbcTemplate.queryForObject(
                "SELECT inventory_quantity FROM products WHERE id = 1",
                Int::class.java,
            )
        assertEquals(0, finalStock)

        val movementCount =
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_movements WHERE product_id = 1 AND movement_type = 'DECREASE'",
                Int::class.java,
            )
        assertEquals(1, movementCount)
    }
}
