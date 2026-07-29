package com.nexus.shopping.integration.checkout

import com.fasterxml.jackson.databind.json.JsonMapper
import com.nexus.shopping.payment.adapter.outbound.jpa.PaymentJpaRepositoryAdapter
import com.nexus.shopping.payment.adapter.outbound.provider.LoggingPaymentProviderGateway
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptRepositoryPort
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptReservation
import com.nexus.shopping.payment.application.port.outbound.PaymentProviderGateway
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingRequest
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingResult
import com.nexus.shopping.payment.domain.PaymentAttempt
import com.nexus.shopping.payment.domain.PaymentStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:payment_checkout_concurrency_http_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@Import(PaymentCheckoutConcurrencyHttpTest.ConcurrencyConfiguration::class)
class PaymentCheckoutConcurrencyHttpTest {
    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var provider: ConcurrentBlockingPaymentProvider

    @Autowired
    private lateinit var attempts: ObservingPaymentAttemptRepository

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    @Test
    fun `concurrent identical checkouts produce one dispatch and consistent terminal responses`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)
        val idempotencyKey = "concurrent-${UUID.randomUUID()}"
        val start = CyclicBarrier(REQUEST_COUNT)
        val executor = Executors.newFixedThreadPool(REQUEST_COUNT)

        try {
            val responses =
                List(REQUEST_COUNT) {
                    executor.submit<HttpResponse<String>> {
                        start.await(10, TimeUnit.SECONDS)
                        checkout(port, customerId, idempotencyKey)
                    }
                }
            assertTrue(provider.entered.await(10, TimeUnit.SECONDS), "Provider was not invoked")
            assertTrue(attempts.requestedReplays.await(10, TimeUnit.SECONDS), "Concurrent replays did not observe REQUESTED")
            provider.release.countDown()

            val completed = responses.map { it.get(15, TimeUnit.SECONDS) }
            assertEquals(listOf(200, 200, 200, 200, 200, 200, 200, 201), completed.map { it.statusCode() }.sorted())
            val orders = completed.map { mapper.readTree(it.body()) }
            assertEquals(setOf("CONFIRMED"), orders.map { it["status"].asText() }.toSet())
            assertEquals(1, orders.map { it["id"].asLong() }.toSet().size)
            val orderId = orders.first()["id"].asLong()
            assertEquals(1, count("SELECT COUNT(*) FROM payment_provider_dispatches WHERE reference_id = ?", "checkout:$orderId"))
            assertEquals(1, count("SELECT COUNT(*) FROM payment_attempts WHERE reference_id = ?", "checkout:$orderId"))
            assertEquals(1, count("SELECT COUNT(*) FROM notifications WHERE reference_id = ?", orderId))
        } finally {
            provider.release.countDown()
            executor.shutdownNow()
        }
    }

    private fun createCustomer(port: String): Long {
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val response =
            post(
                port,
                "/customers",
                """
                {
                  "name": "Concurrent Customer",
                  "document": "$suffix",
                  "documentType": "CPF",
                  "email": "$suffix@example.com",
                  "street": "Rua Teste",
                  "number": "1",
                  "neighborhood": "Centro",
                  "city": "Sao Paulo",
                  "state": "SP",
                  "zipCode": "01001000",
                  "country": "BR"
                }
                """.trimIndent(),
            )
        assertEquals(201, response.statusCode())
        return mapper.readTree(response.body())["id"].asLong()
    }

    private fun addItem(
        port: String,
        customerId: Long,
    ) {
        assertEquals(
            200,
            post(
                port,
                "/customers/$customerId/cart/items",
                """
                {
                  "productId": 10,
                  "productName": "Product 10",
                  "unitPriceAmount": 19.90,
                  "currency": "BRL",
                  "quantity": 2
                }
                """.trimIndent(),
            ).statusCode(),
        )
    }

    private fun checkout(
        port: String,
        customerId: Long,
        idempotencyKey: String,
    ): HttpResponse<String> =
        post(
            port,
            "/customers/$customerId/cart/checkout",
            """
            {
              "customerSnapshot": {
                "name": "Concurrent Customer",
                "document": "12345678900",
                "documentType": "CPF",
                "email": "concurrent@example.com",
                "phone": null
              },
              "shippingAddressSnapshot": {
                "street": "Rua Teste",
                "number": "1",
                "complement": null,
                "neighborhood": "Centro",
                "city": "Sao Paulo",
                "state": "SP",
                "zipCode": "01001000",
                "country": "BR"
              },
              "paymentToken": "approved"
            }
            """.trimIndent(),
            idempotencyKey,
        )

    private fun post(
        port: String,
        path: String,
        body: String,
        idempotencyKey: String? = null,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .header("Content-Type", "application/json")
                .apply {
                    if (idempotencyKey != null) header("Idempotency-Key", idempotencyKey)
                }.POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun count(
        sql: String,
        argument: Any,
    ): Int = requireNotNull(jdbcTemplate.queryForObject(sql, Int::class.java, argument))

    @TestConfiguration
    class ConcurrencyConfiguration {
        @Bean
        @Primary
        fun observingPaymentAttempts(
            @Qualifier("paymentJpaRepositoryAdapter")
            delegate: PaymentJpaRepositoryAdapter,
        ): ObservingPaymentAttemptRepository = ObservingPaymentAttemptRepository(delegate, REQUEST_COUNT - 1)

        @Bean
        @Primary
        fun concurrentBlockingPaymentProvider(
            @Qualifier("loggingPaymentProviderGateway")
            delegate: LoggingPaymentProviderGateway,
        ): ConcurrentBlockingPaymentProvider = ConcurrentBlockingPaymentProvider(delegate)
    }

    private companion object {
        private const val REQUEST_COUNT = 8
    }
}

class ObservingPaymentAttemptRepository(
    private val delegate: PaymentAttemptRepositoryPort,
    replayCount: Int,
) : PaymentAttemptRepositoryPort {
    val requestedReplays = CountDownLatch(replayCount)

    override fun reserve(attempt: PaymentAttempt): PaymentAttemptReservation =
        delegate.reserve(attempt).also { reservation ->
            if (reservation is PaymentAttemptReservation.Existing && reservation.attempt.status == PaymentStatus.REQUESTED) {
                requestedReplays.countDown()
            }
        }

    override fun findByReferenceIdAndIdempotencyKey(
        referenceId: String,
        idempotencyKey: String,
    ): PaymentAttempt? = delegate.findByReferenceIdAndIdempotencyKey(referenceId, idempotencyKey)

    override fun complete(
        attemptReference: String,
        processingLeaseToken: String,
        status: PaymentStatus,
        providerTransactionId: String?,
        completedAt: Instant,
    ): PaymentAttempt? =
        delegate.complete(
            attemptReference,
            processingLeaseToken,
            status,
            providerTransactionId,
            completedAt,
        )
}

class ConcurrentBlockingPaymentProvider(
    private val delegate: PaymentProviderGateway,
) : PaymentProviderGateway {
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)

    override fun process(request: ProviderProcessingRequest): ProviderProcessingResult {
        entered.countDown()
        check(release.await(10, TimeUnit.SECONDS)) { "Timed out waiting to release provider" }
        return delegate.process(request)
    }
}
