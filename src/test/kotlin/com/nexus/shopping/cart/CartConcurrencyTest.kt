package com.nexus.shopping.cart

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves the PRD invariant "a Customer may have at most one ACTIVE cart" holds under real
 * concurrency: many threads racing GET /customers/{customerId}/cart for a brand new customerId
 * (no cart exists yet) must converge onto a single ACTIVE cart row, not one each.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:nexus_shopping_cart_concurrency_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class CartConcurrencyTest {
    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val httpClient = HttpClient.newHttpClient()

    @Test
    fun `test environment disables open entity manager in view to match production`() {
        assertEquals(false, environment.getProperty("spring.jpa.open-in-view", Boolean::class.java))
    }

    @Test
    fun `20 concurrent GET requests for the same new customerId create exactly one ACTIVE cart`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = 8L
        val threadCount = 20
        val executor = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val statusCodes = ConcurrentLinkedQueue<Int>()

        try {
            repeat(threadCount) {
                executor.submit {
                    readyLatch.countDown()
                    startLatch.await()
                    try {
                        val request =
                            HttpRequest
                                .newBuilder()
                                .uri(URI.create("http://localhost:$port/customers/$customerId/cart"))
                                .GET()
                                .build()
                        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                        statusCodes += response.statusCode()
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "Timed out waiting for worker threads to start")
            startLatch.countDown()
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Timed out waiting for concurrent requests to finish")
        } finally {
            executor.shutdown()
        }

        assertEquals(threadCount, statusCodes.size)
        assertTrue(statusCodes.all { it == 200 }, "Expected every concurrent request to succeed with 200, got: $statusCodes")

        val activeCartCount =
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM carts WHERE customer_id = ? AND status = 'ACTIVE'",
                Int::class.java,
                customerId,
            )
        assertEquals(1, activeCartCount, "Expected exactly one ACTIVE cart for customerId $customerId, found $activeCartCount")
    }

    /**
     * Proves that concurrent writes to items on an ALREADY EXISTING cart never leak a 500: unlike
     * cart creation (covered above), this exercises CartJpaRepositoryAdapter.updateCart() - the
     * lock-under-findByIdForUpdate -> reconcile items -> flush path used by
     * Add/Remove/ClearCartUseCase - under real concurrency, mixing reads (GET) with writes (POST
     * add-item) on the same cart.
     */
    @Test
    fun `30 concurrent GET and POST add-item requests on an existing cart never return 500`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = 9L
        val productId = 100L

        // Seed: the cart and an initial item already exist before the concurrent phase, matching
        // the reported scenario (writes racing on an already existing cart).
        postAddItem(port, customerId, productId, quantity = 1)

        val totalThreads = 30
        val postThreads = 15
        val executor = Executors.newFixedThreadPool(totalThreads)
        val readyLatch = CountDownLatch(totalThreads)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(totalThreads)
        val statusCodes = ConcurrentLinkedQueue<Int>()

        try {
            repeat(totalThreads) { index ->
                executor.submit {
                    readyLatch.countDown()
                    startLatch.await()
                    try {
                        val response =
                            if (index < postThreads) {
                                postAddItem(port, customerId, productId, quantity = 1)
                            } else {
                                getCart(port, customerId)
                            }
                        statusCodes += response.statusCode()
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "Timed out waiting for worker threads to start")
            startLatch.countDown()
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Timed out waiting for concurrent requests to finish")
        } finally {
            executor.shutdown()
        }

        assertEquals(totalThreads, statusCodes.size)
        assertTrue(statusCodes.none { it == 500 }, "Expected no 500s, got: $statusCodes")
        assertTrue(statusCodes.all { it == 200 }, "Expected every request to succeed with 200, got: $statusCodes")

        // No lost updates either: the initial seed add (quantity 1) plus the postThreads
        // concurrent adds (quantity 1 each) must all be reflected in the final quantity.
        val finalQuantity =
            jdbcTemplate.queryForObject(
                "SELECT ci.quantity FROM cart_items ci JOIN carts c ON c.id = ci.cart_id " +
                    "WHERE c.customer_id = ? AND c.status = 'ACTIVE' AND ci.product_id = ?",
                Int::class.java,
                customerId,
                productId,
            )
        assertEquals(1 + postThreads, finalQuantity, "Expected no lost updates to productId $productId's quantity")
    }

    private fun getCart(
        port: String,
        customerId: Long,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/customers/$customerId/cart"))
                .GET()
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun postAddItem(
        port: String,
        customerId: Long,
        productId: Long,
        quantity: Int,
    ): HttpResponse<String> {
        val body =
            """
            {
              "productId": $productId,
              "productName": "Product $productId",
              "unitPriceAmount": 19.90,
              "currency": "BRL",
              "quantity": $quantity
            }
            """.trimIndent()
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/customers/$customerId/cart/items"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
