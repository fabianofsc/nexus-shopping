package com.nexus.shopping.integration.checkout

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.nexus.shopping.integration.checkout.application.PaymentReconciliationUseCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:payment_checkout_http_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@EnableWireMock(ConfigureWireMock(baseUrlProperties = ["nexus.payment-service.base-url"]))
class PaymentCheckoutHttpTest {
    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var reconciliation: PaymentReconciliationUseCase

    @InjectWireMock
    private lateinit var wireMock: WireMockServer

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    @Test
    fun `approved payment confirms Order and sends Notification`() {
        stubDispatch("provider-attempt-1")
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)

        val dispatched = checkout(port, customerId, "approved-${UUID.randomUUID()}", "approved")
        assertEquals(202, dispatched.statusCode())
        val orderId = mapper.readTree(dispatched.body())["id"].asLong()

        stubStatus("provider-attempt-1", "APPROVED")
        reconciliation.reconcile()

        val order = getOrder(port, customerId, orderId)
        assertEquals("CONFIRMED", order["status"].asText())
        assertEquals("APPROVED", scalar("SELECT status FROM payment_attempts WHERE reference_id = ?", "checkout:$orderId"))
        assertEquals("SENT", scalar("SELECT status FROM notifications WHERE reference_id = ?", orderId))
    }

    @Test
    fun `rejected payment fails Order without creating Notification`() {
        stubDispatch("provider-attempt-2")
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)

        val dispatched = checkout(port, customerId, "rejected-${UUID.randomUUID()}", "rejected")
        assertEquals(202, dispatched.statusCode())
        val orderId = mapper.readTree(dispatched.body())["id"].asLong()

        stubStatus("provider-attempt-2", "REJECTED")
        reconciliation.reconcile()

        val order = getOrder(port, customerId, orderId)
        assertEquals("PAYMENT_FAILED", order["status"].asText())
        assertEquals("REJECTED", scalar("SELECT status FROM payment_attempts WHERE reference_id = ?", "checkout:$orderId"))
        assertEquals(0, count("SELECT COUNT(*) FROM notifications WHERE reference_id = ?", orderId))
    }

    @Test
    fun `checkout replay reconciles terminal result without a second provider dispatch or Notification`() {
        stubDispatch("provider-attempt-3")
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)
        val idempotencyKey = "replay-${UUID.randomUUID()}"

        val created = checkout(port, customerId, idempotencyKey, "approved")
        assertEquals(202, created.statusCode())
        val orderId = mapper.readTree(created.body())["id"].asLong()

        stubStatus("provider-attempt-3", "APPROVED")
        reconciliation.reconcile()

        val replay = checkout(port, customerId, idempotencyKey, "approved")

        assertEquals(200, replay.statusCode())
        val replayedOrder = mapper.readTree(replay.body())
        assertEquals(orderId, replayedOrder["id"].asLong())
        assertEquals("CONFIRMED", replayedOrder["status"].asText())
        wireMock.verify(1, postRequestedFor(urlEqualTo("/v1/payments")))
        assertEquals(1, count("SELECT COUNT(*) FROM notifications WHERE reference_id = ?", orderId))
    }

    @Test
    fun `same checkout key with a different token returns conflict without another dispatch`() {
        stubDispatch("provider-attempt-4")
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)
        val idempotencyKey = "token-conflict-${UUID.randomUUID()}"

        val created = checkout(port, customerId, idempotencyKey, "approved")
        val conflict = checkout(port, customerId, idempotencyKey, "different-token")

        assertEquals(202, created.statusCode())
        assertEquals(409, conflict.statusCode())
        wireMock.verify(1, postRequestedFor(urlEqualTo("/v1/payments")))

        // Drain this test's own dispatched attempt so it doesn't linger as REQUESTED and get
        // swept up (and fail on a missing stub) by another test's reconciliation.reconcile() call
        // against this class's shared H2 database.
        stubStatus("provider-attempt-4", "APPROVED")
        reconciliation.reconcile()
    }

    @Test
    fun `invalid trusted total rolls back before creating Order or confirming Cart`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId, unitPriceAmount = "9999999999.99", quantity = 2)

        val response = checkout(port, customerId, "invalid-total-${UUID.randomUUID()}", "approved")

        assertEquals(400, response.statusCode())
        assertEquals(0, count("SELECT COUNT(*) FROM orders WHERE customer_id = ?", customerId))
        assertEquals("ACTIVE", scalar("SELECT status FROM carts WHERE customer_id = ?", customerId))
    }

    private fun stubDispatch(providerAttemptReference: String) {
        wireMock.stubFor(
            post(urlEqualTo("/v1/payments"))
                .willReturn(
                    aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """{"attemptReference":"$providerAttemptReference","referenceId":"irrelevant","status":"PROCESSING","replayed":false}""",
                        ),
                ),
        )
    }

    private fun stubStatus(
        providerAttemptReference: String,
        status: String,
    ) {
        wireMock.stubFor(
            get(urlEqualTo("/v1/payments/$providerAttemptReference"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"attemptReference":"$providerAttemptReference","status":"$status"}"""),
                ),
        )
    }

    private fun getOrder(
        port: String,
        customerId: Long,
        orderId: Long,
    ) = mapper.readTree(
        httpClient
            .send(
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("http://localhost:$port/customers/$customerId/orders/$orderId"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString(),
            ).body(),
    )

    private fun createCustomer(port: String): Long {
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val response =
            post(
                port,
                "/customers",
                """
                {
                  "name": "Payment Customer",
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
        unitPriceAmount: String = "19.90",
        quantity: Int = 2,
    ) {
        val response =
            post(
                port,
                "/customers/$customerId/cart/items",
                """
                {
                  "productId": 10,
                  "productName": "Product 10",
                  "unitPriceAmount": $unitPriceAmount,
                  "currency": "BRL",
                  "quantity": $quantity
                }
                """.trimIndent(),
            )
        assertEquals(200, response.statusCode())
    }

    private fun checkout(
        port: String,
        customerId: Long,
        idempotencyKey: String,
        paymentToken: String,
    ): HttpResponse<String> =
        post(
            port,
            "/customers/$customerId/cart/checkout",
            """
            {
              "customerSnapshot": {
                "name": "Payment Customer",
                "document": "12345678900",
                "documentType": "CPF",
                "email": "payment@example.com",
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
              "paymentToken": "$paymentToken"
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
}
