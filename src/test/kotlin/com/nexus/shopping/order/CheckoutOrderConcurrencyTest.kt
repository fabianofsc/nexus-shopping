package com.nexus.shopping.order

import com.nexus.shopping.cart.adapter.outbound.jpa.CartJpaRepositoryAdapter
import com.nexus.shopping.cart.domain.CartItem
import com.nexus.shopping.cart.domain.ProductSummary
import com.nexus.shopping.order.adapter.outbound.jpa.JpaTransactionAdapter
import com.nexus.shopping.order.adapter.outbound.jpa.OrderJpaRepositoryAdapter
import com.nexus.shopping.order.application.command.CheckoutOrderCommand
import com.nexus.shopping.order.application.usecase.CheckoutOrderUseCase
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
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
    private lateinit var orders: OrderJpaRepositoryAdapter

    @Autowired
    private lateinit var carts: CartJpaRepositoryAdapter

    @Autowired
    private lateinit var transactions: JpaTransactionAdapter

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `concurrent same-key checkouts create one order and replay it after the cart closes`() {
        prepareCart(3L)

        val results = concurrently(12) { CheckoutOrderUseCase(orders, carts, transactions).execute(command(3L, "same-key")) }

        assertEquals(12, results.size)
        assertTrue(results.all { it.isSuccess }, "Expected all idempotent requests to succeed: $results")
        assertEquals(1, results.map { it.getOrThrow().id }.toSet().size)
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE customer_id = 3", Int::class.java))
        assertEquals("CHECKED_OUT", jdbcTemplate.queryForObject("SELECT status FROM carts WHERE customer_id = 3", String::class.java))
    }

    @Test
    fun `concurrent different-key checkouts allow only one cart checkout to win`() {
        prepareCart(4L)

        val results = concurrently(12) { index -> CheckoutOrderUseCase(orders, carts, transactions).execute(command(4L, "key-$index")) }

        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE customer_id = 4", Int::class.java))
        assertEquals("CHECKED_OUT", jdbcTemplate.queryForObject("SELECT status FROM carts WHERE customer_id = 4", String::class.java))
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

    private fun command(
        customerId: Long,
        idempotencyKey: String,
    ) = CheckoutOrderCommand(
        customerId = customerId,
        customerSnapshot = CustomerSnapshot(customerId, "Ana Silva", "12345678900", "CPF", "ana@example.com", null),
        shippingAddressSnapshot = ShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
        idempotencyKey = idempotencyKey,
    )

    private fun concurrently(
        threads: Int,
        action: (Int) -> Order,
    ): List<Result<Order>> {
        val executor = Executors.newFixedThreadPool(threads)
        val ready = CountDownLatch(threads)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        val results = ConcurrentLinkedQueue<Result<Order>>()
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
