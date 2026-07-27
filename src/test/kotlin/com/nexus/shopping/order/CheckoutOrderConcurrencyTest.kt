package com.nexus.shopping.order

import com.nexus.shopping.cart.adapter.outbound.jpa.CartJpaRepositoryAdapter
import com.nexus.shopping.cart.domain.CartItem
import com.nexus.shopping.cart.domain.ProductSummary
import com.nexus.shopping.integration.checkout.application.CheckoutWorkflowUseCase
import com.nexus.shopping.integration.checkout.application.model.CheckoutCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutCustomerData
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData
import com.nexus.shopping.integration.checkout.application.model.CheckoutShippingAddressData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.nexus.shopping.cart.domain.Currency as CartCurrency

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:order_checkout_concurrency_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class CheckoutOrderConcurrencyTest {
    @Autowired
    private lateinit var carts: CartJpaRepositoryAdapter

    @Autowired
    private lateinit var checkout: CheckoutWorkflowUseCase

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `concurrent same-key checkouts create one order and replay it after the cart closes`() {
        prepareCart(3L)

        val results =
            concurrently(12) { checkout.execute(command(3L, "same-key")) }

        assertEquals(12, results.size)
        assertTrue(results.all { it.isSuccess }, "Expected all idempotent requests to succeed: $results")
        assertEquals(1, results.map { it.getOrThrow().id }.toSet().size)
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE customer_id = 3", Int::class.java))
        assertEquals("CHECKED_OUT", jdbcTemplate.queryForObject("SELECT status FROM carts WHERE customer_id = 3", String::class.java))
    }

    @Test
    fun `concurrent different-key checkouts allow only one cart checkout to win`() {
        prepareCart(4L)

        val results =
            concurrently(
                12,
            ) { index -> checkout.execute(command(4L, "key-$index")) }

        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE customer_id = 4", Int::class.java))
        assertEquals("CHECKED_OUT", jdbcTemplate.queryForObject("SELECT status FROM carts WHERE customer_id = 4", String::class.java))
    }

    @Test
    fun `concurrent replays keep a new active cart open after the original cart was checked out`() {
        val customerId = 5L
        prepareCart(customerId)
        val original = checkout.execute(command(customerId, "original-key"))
        val newCartId = createFreshActiveCart(customerId)

        val results = concurrently(12) { checkout.execute(command(customerId, "original-key")) }

        assertTrue(results.all { it.isSuccess }, "Expected all old-key replays to succeed: $results")
        assertEquals(setOf(original.id), results.map { it.getOrThrow().id }.toSet())
        assertEquals(
            "ACTIVE",
            jdbcTemplate.queryForObject("SELECT status FROM carts WHERE id = ?", String::class.java, newCartId),
        )
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE customer_id = ?", Int::class.java, customerId))
    }

    private fun prepareCart(customerId: Long) {
        val cart = carts.getOrCreateActiveByCustomerId(customerId)
        carts.updateCart(requireNotNull(cart.id)) {
            it.copy(
                items =
                    listOf(
                        CartItem(
                            ProductSummary(10L, "Produto 10", BigDecimal("19.90"), CartCurrency.BRL),
                            quantity = 2,
                        ),
                    ),
            )
        }
    }

    private fun createFreshActiveCart(customerId: Long): Long {
        jdbcTemplate.update("INSERT INTO carts (customer_id, status) VALUES (?, 'ACTIVE')", customerId)
        val cartId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM carts WHERE customer_id = ?", Long::class.java, customerId)
        jdbcTemplate.update(
            """
            INSERT INTO cart_items (cart_id, product_id, product_name, unit_price_amount, currency, quantity)
            VALUES (?, 10, 'Produto 10', 19.90, 'BRL', 2)
            """.trimIndent(),
            cartId,
        )
        return requireNotNull(cartId)
    }

    private fun command(
        customerId: Long,
        idempotencyKey: String,
    ) = CheckoutCommand(
        customerId = customerId,
        customerSnapshot = CheckoutCustomerData(customerId, "Ana Silva", "12345678900", "CPF", "ana@example.com", null),
        shippingAddressSnapshot =
            CheckoutShippingAddressData("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
        idempotencyKey = idempotencyKey,
    )

    private fun concurrently(
        threads: Int,
        action: (Int) -> CheckoutOrderData,
    ): List<Result<CheckoutOrderData>> {
        val executor = Executors.newFixedThreadPool(threads)
        val ready = CountDownLatch(threads)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        val results = ConcurrentLinkedQueue<Result<CheckoutOrderData>>()
        try {
            repeat(threads) { index ->
                executor.submit {
                    ready.countDown()
                    start.await()
                    try {
                        results += runCatching { action(index) }
                    } finally {
                        done.countDown()
                    }
                }
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Timed out waiting for workers")
            start.countDown()
            assertTrue(done.await(30, TimeUnit.SECONDS), "Timed out waiting for workers")
        } finally {
            executor.shutdown()
        }
        return results.toList()
    }
}
