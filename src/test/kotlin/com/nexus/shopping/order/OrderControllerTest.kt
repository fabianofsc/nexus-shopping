package com.nexus.shopping.order

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
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:nexus_shopping_order_controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class OrderControllerTest {
    @Autowired
    private lateinit var environment: Environment

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    @Test
    fun `POST checkout returns 201 with the order created from cart items`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/1/cart/items", addItemBody())

        val response = post(port, "/customers/1/cart/checkout", checkoutBody(), "checkout-1")

        assertEquals(201, response.statusCode())
        val order = mapper.readTree(response.body())
        assertEquals(1L, order["customerId"].asLong())
        assertEquals("WAITING_PAYMENT", order["status"].asText())
        assertEquals(1, order["items"].size())
    }

    @Test
    fun `POST checkout replays the original order with 200 for the same idempotency key and payload`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/2/cart/items", addItemBody())

        val created = post(port, "/customers/2/cart/checkout", checkoutBody(), "checkout-replay")
        val replay = post(port, "/customers/2/cart/checkout", checkoutBody(), "checkout-replay")

        assertEquals(201, created.statusCode())
        assertEquals(200, replay.statusCode())
        assertEquals(mapper.readTree(created.body())["id"].asLong(), mapper.readTree(replay.body())["id"].asLong())
    }

    @Test
    fun `POST checkout requires the Idempotency-Key header`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/3/cart/items", addItemBody())

        val response = post(port, "/customers/3/cart/checkout", checkoutBody())

        assertProblemDetail(response, 400, "Bad Request", "/customers/3/cart/checkout")
    }

    @Test
    fun `POST checkout returns 409 problem details when the key is reused with a different payload`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/3/cart/items", addItemBody())
        post(port, "/customers/3/cart/checkout", checkoutBody(), "checkout-conflict")

        val response = post(port, "/customers/3/cart/checkout", checkoutBody(number = "999"), "checkout-conflict")

        assertProblemDetail(response, 409, "Conflict", "/customers/3/cart/checkout")
    }

    @Test
    fun `POST checkout returns 400 for an empty cart`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port, "empty-cart")

        val response = post(port, "/customers/$customerId/cart/checkout", checkoutBody(), "checkout-empty")

        assertProblemDetail(response, 400, "Bad Request", "/customers/$customerId/cart/checkout")
    }

    @Test
    fun `POST checkout returns 400 for a cart already checked out`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port, "closed-cart")
        post(port, "/customers/$customerId/cart/items", addItemBody())
        post(port, "/customers/$customerId/cart/checkout", checkoutBody(), "checkout-closed-original")

        val response = post(port, "/customers/$customerId/cart/checkout", checkoutBody(), "checkout-closed-new-key")

        assertProblemDetail(response, 400, "Bad Request", "/customers/$customerId/cart/checkout")
    }

    @Test
    fun `GET order detail returns the order for its customer`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/4/cart/items", addItemBody())
        val created = mapper.readTree(post(port, "/customers/4/cart/checkout", checkoutBody(), "checkout-detail").body())

        val response = get(port, "/customers/4/orders/${created["id"].asLong()}")

        assertEquals(200, response.statusCode())
        val order = mapper.readTree(response.body())
        assertEquals(created["id"].asLong(), order["id"].asLong())
        assertEquals(4L, order["customerId"].asLong())
    }

    @Test
    fun `GET order detail returns 404 when the order belongs to another customer`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/5/cart/items", addItemBody())
        val created = mapper.readTree(post(port, "/customers/5/cart/checkout", checkoutBody(), "checkout-owner").body())

        val response = get(port, "/customers/6/orders/${created["id"].asLong()}")

        assertProblemDetail(response, 404, "Not Found", "/customers/6/orders/${created["id"].asLong()}")
    }

    @Test
    fun `GET orders returns a Slice page for the customer`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/7/cart/items", addItemBody())
        post(port, "/customers/7/cart/checkout", checkoutBody(), "checkout-list")

        val response = get(port, "/customers/7/orders?page=0&size=50")

        assertEquals(200, response.statusCode())
        val page = mapper.readTree(response.body())
        assertEquals(0, page["page"].asInt())
        assertEquals(50, page["size"].asInt())
        assertEquals(1, page["content"].size())
        assertEquals(false, page["hasNext"].asBoolean())
    }

    @Test
    fun `POST cancel changes a waiting order to CANCELLED`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/8/cart/items", addItemBody())
        val created = mapper.readTree(post(port, "/customers/8/cart/checkout", checkoutBody(), "checkout-cancel").body())

        val response = post(port, "/customers/8/orders/${created["id"].asLong()}/cancel", "{}")

        assertEquals(200, response.statusCode())
        assertEquals("CANCELLED", mapper.readTree(response.body())["status"].asText())
    }

    @Test
    fun `POST cancel returns 409 problem details when the order cannot transition again`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/9/cart/items", addItemBody())
        val created = mapper.readTree(post(port, "/customers/9/cart/checkout", checkoutBody(), "checkout-cancel-conflict").body())
        post(port, "/customers/9/orders/${created["id"].asLong()}/cancel", "{}")

        val response = post(port, "/customers/9/orders/${created["id"].asLong()}/cancel", "{}")

        assertProblemDetail(response, 409, "Conflict", "/customers/9/orders/${created["id"].asLong()}/cancel")
    }

    @Test
    fun `POST cancel returns 404 when the order belongs to another customer`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/10/cart/items", addItemBody())
        val created = mapper.readTree(post(port, "/customers/10/cart/checkout", checkoutBody(), "checkout-cancel-owner").body())

        val response = post(port, "/customers/11/orders/${created["id"].asLong()}/cancel", "{}")

        assertProblemDetail(response, 404, "Not Found", "/customers/11/orders/${created["id"].asLong()}/cancel")
    }

    @Test
    fun `POST cart items returns 400 after checkout`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port, "cart-add")
        post(port, "/customers/$customerId/cart/items", addItemBody())
        post(port, "/customers/$customerId/cart/checkout", checkoutBody(), "checkout-cart-add")

        val response = post(port, "/customers/$customerId/cart/items", addItemBody(productId = 20))

        assertProblemDetail(response, 400, "Bad Request", "/customers/$customerId/cart/items")
    }

    @Test
    fun `DELETE cart item returns 400 after checkout`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port, "cart-remove")
        post(port, "/customers/$customerId/cart/items", addItemBody())
        post(port, "/customers/$customerId/cart/checkout", checkoutBody(), "checkout-cart-remove")

        val response = delete(port, "/customers/$customerId/cart/items/10")

        assertProblemDetail(response, 400, "Bad Request", "/customers/$customerId/cart/items/10")
    }

    @Test
    fun `DELETE cart items returns 400 after checkout`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port, "cart-clear")
        post(port, "/customers/$customerId/cart/items", addItemBody())
        post(port, "/customers/$customerId/cart/checkout", checkoutBody(), "checkout-cart-clear")

        val response = delete(port, "/customers/$customerId/cart/items")

        assertProblemDetail(response, 400, "Bad Request", "/customers/$customerId/cart/items")
    }

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

    private fun delete(
        port: String,
        path: String,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .DELETE()
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun createCustomer(
        port: String,
        suffix: String,
    ): Long {
        val body =
            """
            {
              "name": "Customer $suffix",
              "document": "$suffix-document",
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
            """.trimIndent()
        val response = post(port, "/customers", body)
        assertEquals(201, response.statusCode())
        return mapper.readTree(response.body())["id"].asLong()
    }

    private fun addItemBody(productId: Long = 10L) =
        """
        {
          "productId": $productId,
          "productName": "Product 10",
          "unitPriceAmount": 19.90,
          "currency": "BRL",
          "quantity": 2
        }
        """.trimIndent()

    private fun assertProblemDetail(
        response: HttpResponse<String>,
        expectedStatus: Int,
        expectedTitle: String,
        expectedInstance: String,
    ) {
        assertEquals(expectedStatus, response.statusCode())
        assertTrue(
            response
                .headers()
                .firstValue("Content-Type")
                .orElse("")
                .startsWith("application/problem+json"),
        )
        val problem = mapper.readTree(response.body())
        assertEquals("about:blank", problem["type"].asText())
        assertEquals(expectedTitle, problem["title"].asText())
        assertEquals(expectedStatus, problem["status"].asInt())
        assertEquals(expectedInstance, problem["instance"].asText())
    }

    private fun checkoutBody(number: String = "123") =
        """
        {
          "customerSnapshot": {
            "name": "Ana Silva",
            "document": "12345678900",
            "documentType": "CPF",
            "email": "ana@example.com",
            "phone": "+5511999990000"
          },
          "shippingAddressSnapshot": {
            "street": "Rua das Flores",
            "number": "$number",
            "complement": "Apto 45",
            "neighborhood": "Centro",
            "city": "Sao Paulo",
            "state": "SP",
            "zipCode": "01001000",
            "country": "BR"
          }
        }
        """.trimIndent()
}
