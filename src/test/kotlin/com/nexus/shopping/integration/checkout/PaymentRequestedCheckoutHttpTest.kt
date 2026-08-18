package com.nexus.shopping.integration.checkout

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
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
        "spring.datasource.url=jdbc:h2:mem:payment_requested_checkout_http_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@EnableWireMock(ConfigureWireMock(baseUrlProperties = ["nexus.payment-service.base-url"]))
class PaymentRequestedCheckoutHttpTest {
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
    fun `checkout responds WAITING_PAYMENT immediately then confirms once reconciliation observes an approved status`() {
        stubDispatch("provider-attempt-1")
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)
        val idempotencyKey = "requested-${UUID.randomUUID()}"

        val dispatched = checkout(port, customerId, idempotencyKey)

        assertEquals(202, dispatched.statusCode())
        val order = mapper.readTree(dispatched.body())
        assertEquals("WAITING_PAYMENT", order["status"].asText())
        assertEquals(
            "REQUESTED",
            scalar("SELECT status FROM payment_attempts WHERE reference_id = ?", "checkout:${order["id"].asLong()}"),
        )
        assertEquals(0, count("SELECT COUNT(*) FROM notifications WHERE reference_id = ?", order["id"].asLong()))

        val replayWhileProcessing = checkout(port, customerId, idempotencyKey)
        assertEquals(202, replayWhileProcessing.statusCode())
        assertEquals("WAITING_PAYMENT", mapper.readTree(replayWhileProcessing.body())["status"].asText())

        stubStatus("provider-attempt-1", "APPROVED")
        reconciliation.reconcile()

        val confirmedReplay = checkout(port, customerId, idempotencyKey)
        assertEquals(200, confirmedReplay.statusCode())
        assertEquals("CONFIRMED", mapper.readTree(confirmedReplay.body())["status"].asText())
        assertEquals(1, count("SELECT COUNT(*) FROM notifications WHERE reference_id = ?", order["id"].asLong()))
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
        jdbcTemplate.seedStockedProduct()
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
}
