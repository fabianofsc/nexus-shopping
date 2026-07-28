package com.nexus.shopping.integration.checkout

import com.fasterxml.jackson.databind.json.JsonMapper
import com.nexus.shopping.payment.adapter.outbound.provider.LoggingPaymentProviderGateway
import com.nexus.shopping.payment.application.port.outbound.PaymentProviderGateway
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingRequest
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingResult
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
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:payment_requested_checkout_http_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@Import(PaymentRequestedCheckoutHttpTest.BlockingProviderConfiguration::class)
class PaymentRequestedCheckoutHttpTest {
    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var provider: BlockingPaymentProvider

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    @Test
    fun `valid payment lease times out as 202 with WAITING_PAYMENT and reconciles later`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)
        val idempotencyKey = "requested-${UUID.randomUUID()}"
        val executor = Executors.newSingleThreadExecutor()

        try {
            val owner = executor.submit<HttpResponse<String>> { checkout(port, customerId, idempotencyKey) }
            assertTrue(provider.entered.await(10, TimeUnit.SECONDS), "Provider was not invoked")

            val waiting = checkout(port, customerId, idempotencyKey)

            assertEquals(202, waiting.statusCode())
            val waitingOrder = mapper.readTree(waiting.body())
            assertEquals("WAITING_PAYMENT", waitingOrder["status"].asText())
            assertEquals(
                "REQUESTED",
                scalar("SELECT status FROM payment_attempts WHERE reference_id = ?", "checkout:${waitingOrder["id"].asLong()}"),
            )
            assertEquals(0, count("SELECT COUNT(*) FROM notifications WHERE reference_id = ?", waitingOrder["id"].asLong()))

            provider.release.countDown()
            val completed = owner.get(10, TimeUnit.SECONDS)
            assertEquals(201, completed.statusCode())
            assertEquals("CONFIRMED", mapper.readTree(completed.body())["status"].asText())
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
                  "name": "Requested Customer",
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
                "name": "Requested Customer",
                "document": "12345678900",
                "documentType": "CPF",
                "email": "requested@example.com",
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

    private fun scalar(
        sql: String,
        argument: Any,
    ): String = requireNotNull(jdbcTemplate.queryForObject(sql, String::class.java, argument))

    private fun count(
        sql: String,
        argument: Any,
    ): Int = requireNotNull(jdbcTemplate.queryForObject(sql, Int::class.java, argument))

    @TestConfiguration
    class BlockingProviderConfiguration {
        @Bean
        @Primary
        fun blockingPaymentProvider(
            @Qualifier("loggingPaymentProviderGateway")
            delegate: LoggingPaymentProviderGateway,
        ): BlockingPaymentProvider = BlockingPaymentProvider(delegate)
    }
}

class BlockingPaymentProvider(
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
