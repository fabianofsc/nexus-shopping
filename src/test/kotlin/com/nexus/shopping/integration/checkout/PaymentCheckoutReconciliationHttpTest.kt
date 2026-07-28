package com.nexus.shopping.integration.checkout

import com.fasterxml.jackson.databind.json.JsonMapper
import com.nexus.shopping.integration.checkout.adapter.outbound.acl.NotificationGatewayAdapter
import com.nexus.shopping.integration.checkout.adapter.outbound.acl.OrderPaymentResultGatewayAdapter
import com.nexus.shopping.integration.checkout.adapter.outbound.acl.PaymentProcessingGatewayAdapter
import com.nexus.shopping.integration.checkout.application.model.ApplyOrderPaymentResultData
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData
import com.nexus.shopping.integration.checkout.application.model.OrderConfirmationNotificationData
import com.nexus.shopping.integration.checkout.application.model.PaymentProcessingData
import com.nexus.shopping.integration.checkout.application.model.PaymentResultData
import com.nexus.shopping.integration.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentProcessingGateway
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:payment_checkout_reconciliation_http_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@Import(PaymentCheckoutReconciliationHttpTest.FailureWindowConfiguration::class)
class PaymentCheckoutReconciliationHttpTest {
    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    @Test
    fun `replays reconcile Payment then Order then Notification across failure windows`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)
        val idempotencyKey = "reconcile-${UUID.randomUUID()}"

        val paymentPersisted = checkout(port, customerId, idempotencyKey)
        val orderConfirmed = checkout(port, customerId, idempotencyKey)
        val notificationSent = checkout(port, customerId, idempotencyKey)

        assertEquals(500, paymentPersisted.statusCode())
        assertEquals(500, orderConfirmed.statusCode())
        assertEquals(200, notificationSent.statusCode())
        val order = mapper.readTree(notificationSent.body())
        assertEquals("CONFIRMED", order["status"].asText())
        assertEquals(
            1,
            count("SELECT COUNT(*) FROM payment_provider_dispatches WHERE reference_id = ?", "checkout:${order["id"].asLong()}"),
        )
        assertEquals(1, count("SELECT COUNT(*) FROM notifications WHERE reference_id = ?", order["id"].asLong()))
        assertEquals("SENT", scalar("SELECT status FROM notifications WHERE reference_id = ?", order["id"].asLong()))
    }

    @Test
    fun `replay with different token conflicts after Order commit and before Payment attempt`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)
        val idempotencyKey = "before-payment-${UUID.randomUUID()}"

        val orderCommitted = checkout(port, customerId, idempotencyKey, FAIL_BEFORE_PAYMENT_TOKEN)
        val incompatibleReplay = checkout(port, customerId, idempotencyKey, "different-token")

        assertEquals(500, orderCommitted.statusCode())
        assertEquals(1, count("SELECT COUNT(*) FROM orders WHERE customer_id = ?", customerId))
        assertEquals(0, count("SELECT COUNT(*) FROM payment_attempts WHERE idempotency_key = ?", idempotencyKey))
        val orderId =
            requireNotNull(jdbcTemplate.queryForObject("SELECT id FROM orders WHERE customer_id = ?", Long::class.java, customerId))
        assertEquals(409, incompatibleReplay.statusCode())
        assertEquals(0, count("SELECT COUNT(*) FROM payment_attempts WHERE idempotency_key = ?", idempotencyKey))
        assertEquals(0, count("SELECT COUNT(*) FROM payment_provider_dispatches WHERE reference_id = ?", "checkout:$orderId"))
    }

    private fun createCustomer(port: String): Long {
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val response =
            post(
                port,
                "/customers",
                """
                {
                  "name": "Reconciliation Customer",
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
        paymentToken: String = "approved",
    ): HttpResponse<String> =
        post(
            port,
            "/customers/$customerId/cart/checkout",
            checkoutBody(paymentToken),
            idempotencyKey,
        )

    private fun checkoutBody(paymentToken: String) =
        """
        {
          "customerSnapshot": {
            "name": "Reconciliation Customer",
            "document": "12345678900",
            "documentType": "CPF",
            "email": "reconciliation@example.com",
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
        """.trimIndent()

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
    class FailureWindowConfiguration {
        @Bean
        @Primary
        fun failBeforePaymentAttempt(
            @Qualifier("paymentProcessingGatewayAdapter")
            delegate: PaymentProcessingGatewayAdapter,
        ): PaymentProcessingGateway =
            object : PaymentProcessingGateway {
                private val first = AtomicBoolean(true)

                override fun process(data: PaymentProcessingData): PaymentResultData {
                    if (data.paymentToken == FAIL_BEFORE_PAYMENT_TOKEN && first.compareAndSet(true, false)) {
                        throw IllegalStateException("failure before Payment attempt")
                    }
                    return delegate.process(data)
                }
            }

        @Bean
        @Primary
        fun failFirstOrderResult(
            @Qualifier("orderPaymentResultGatewayAdapter")
            delegate: OrderPaymentResultGatewayAdapter,
        ): OrderPaymentResultGateway =
            object : OrderPaymentResultGateway {
                private val first = AtomicBoolean(true)

                override fun apply(data: ApplyOrderPaymentResultData): CheckoutOrderData {
                    if (first.compareAndSet(true, false)) throw IllegalStateException("failure before Order result")
                    return delegate.apply(data)
                }
            }

        @Bean
        @Primary
        fun failFirstNotification(
            @Qualifier("notificationGatewayAdapter")
            delegate: NotificationGatewayAdapter,
        ): NotificationGateway =
            object : NotificationGateway {
                private val first = AtomicBoolean(true)

                override fun ensureOrderConfirmation(data: OrderConfirmationNotificationData) {
                    if (first.compareAndSet(true, false)) throw IllegalStateException("failure before Notification")
                    delegate.ensureOrderConfirmation(data)
                }
            }
    }

    private companion object {
        private const val FAIL_BEFORE_PAYMENT_TOKEN = "fail-before-payment"
    }
}
