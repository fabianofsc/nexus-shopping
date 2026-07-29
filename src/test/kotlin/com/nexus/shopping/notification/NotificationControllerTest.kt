package com.nexus.shopping.notification

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:nexus_shopping_notification_controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class NotificationControllerTest {
    @Autowired
    private lateinit var environment: Environment

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    private fun post(
        port: String,
        body: String,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/notifications"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun get(
        port: String,
        path: String,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .GET()
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun assertExceptionDetail(
        response: HttpResponse<String>,
        expectedStatus: Int,
        expectedTitle: String,
        expectedInstance: String,
    ): JsonNode {
        assertEquals(expectedStatus, response.statusCode())
        assertTrue(
            response
                .headers()
                .firstValue("Content-Type")
                .orElse("")
                .startsWith("application/problem+json"),
        )
        val exceptionDetail = mapper.readTree(response.body())
        assertEquals("about:blank", exceptionDetail["type"].asText())
        assertEquals(expectedTitle, exceptionDetail["title"].asText())
        assertEquals(expectedStatus, exceptionDetail["status"].asInt())
        assertEquals(expectedInstance, exceptionDetail["instance"].asText())
        return exceptionDetail
    }

    @Test
    fun `POST notifications returns 201 with sent notification`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "customerId": 1,
              "notificationKey": "controller-order-confirmed-123",
              "recipientEmail": "cliente@example.com",
              "type": "ORDER_CONFIRMED",
              "referenceId": 123,
              "templateParams": { "orderId": "123", "amount": "99.90" }
            }
            """.trimIndent()

        val response = post(port, body)

        assertEquals(201, response.statusCode())
        val notification = mapper.readTree(response.body())
        assertNotNull(notification["id"].asLong().takeIf { it > 0 }, "Expected a generated id > 0")
        assertEquals(1L, notification["customerId"].asLong())
        assertEquals("cliente@example.com", notification["recipientEmail"].asText())
        assertEquals("ORDER_CONFIRMED", notification["type"].asText())
        assertEquals("EMAIL", notification["channel"].asText())
        assertEquals("SENT", notification["status"].asText())
        assertEquals("Pedido 123 confirmado", notification["subject"].asText())
        assertEquals("Seu pedido 123 no valor de 99.90 foi confirmado.", notification["body"].asText())
        assertEquals(123L, notification["referenceId"].asLong())
        assertNotNull(notification["sentAt"].asText())
    }

    @Test
    fun `POST notifications with missing template param returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "customerId": 1,
              "notificationKey": "controller-missing-param",
              "recipientEmail": "cliente@example.com",
              "type": "ORDER_CONFIRMED",
              "templateParams": { "orderId": "123" }
            }
            """.trimIndent()

        val response = post(port, body)

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/notifications",
        )
    }

    @Test
    fun `POST notifications with non-existent customerId returns 400 problem details instead of 500`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "customerId": 999999,
              "notificationKey": "controller-ghost",
              "recipientEmail": "ghost@example.com",
              "type": "ORDER_CONFIRMED",
              "templateParams": { "orderId": "1", "amount": "1.00" }
            }
            """.trimIndent()

        val response = post(port, body)

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/notifications",
        )
    }

    @Test
    fun `GET notification by id returns 200`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "customerId": 2,
              "notificationKey": "controller-order-cancelled-456",
              "recipientEmail": "outro@example.com",
              "type": "ORDER_CANCELLED",
              "templateParams": { "orderId": "456" }
            }
            """.trimIndent()
        val created = mapper.readTree(post(port, body).body())

        val response = get(port, "/notifications/${created["id"].asLong()}")

        assertEquals(200, response.statusCode())
        val notification = mapper.readTree(response.body())
        assertEquals(created["id"].asLong(), notification["id"].asLong())
        assertEquals("Pedido 456 cancelado", notification["subject"].asText())
    }

    @Test
    fun `GET notification by id with non-existent id returns 404 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "/notifications/9999999999")

        assertExceptionDetail(
            response = response,
            expectedStatus = 404,
            expectedTitle = "Not Found",
            expectedInstance = "/notifications/9999999999",
        )
    }

    @Test
    fun `GET notifications without customerId returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "/notifications")

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/notifications",
        )
    }

    @Test
    fun `GET notifications by customerId returns paginated content`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "customerId": 3,
              "notificationKey": "controller-payment-failed-789",
              "recipientEmail": "terceiro@example.com",
              "type": "ORDER_PAYMENT_FAILED",
              "templateParams": { "orderId": "789", "amount": "10.00" }
            }
            """.trimIndent()
        post(port, body)

        val response = get(port, "/notifications?customerId=3&page=0&size=50")

        assertEquals(200, response.statusCode())
        val page = mapper.readTree(response.body())
        assertEquals(0, page["page"].asInt())
        assertEquals(50, page["size"].asInt())
        assertTrue(page["content"].isArray)
        assertTrue(page["content"].size() >= 1)
        assertEquals(false, page["hasNext"].asBoolean())
    }

    @Test
    fun `GET notifications by customerId reports hasNext true when more rows exist than the page size`() {
        val port = environment.getRequiredProperty("local.server.port")
        repeat(3) { index ->
            val body =
                """
                {
                  "customerId": 4,
                  "notificationKey": "controller-page-$index",
                  "recipientEmail": "quarto-$index@example.com",
                  "type": "ORDER_CONFIRMED",
                  "templateParams": { "orderId": "$index", "amount": "1.00" }
                }
                """.trimIndent()
            post(port, body)
        }

        val response = get(port, "/notifications?customerId=4&page=0&size=2")

        assertEquals(200, response.statusCode())
        val page = mapper.readTree(response.body())
        assertEquals(0, page["page"].asInt())
        assertEquals(2, page["size"].asInt())
        assertEquals(2, page["content"].size())
        assertEquals(true, page["hasNext"].asBoolean())
    }
}
