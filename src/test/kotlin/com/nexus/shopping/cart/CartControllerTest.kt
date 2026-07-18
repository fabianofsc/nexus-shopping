package com.nexus.shopping.cart

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
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:nexus_shopping_cart_controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class CartControllerTest {
    @Autowired
    private lateinit var environment: Environment

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

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

    private fun post(
        port: String,
        path: String,
        body: String,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
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

    private fun addItemBody(
        productId: Long = 10L,
        productName: String = "Product 10",
        unitPriceAmount: String = "19.90",
        currency: String = "BRL",
        quantity: Int = 2,
    ) = """
        {
          "productId": $productId,
          "productName": "$productName",
          "unitPriceAmount": $unitPriceAmount,
          "currency": "$currency",
          "quantity": $quantity
        }
        """.trimIndent()

    @Test
    fun `GET customer cart returns 200 and creates an empty ACTIVE cart when none exists`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "/customers/1/cart")

        assertEquals(200, response.statusCode())
        val cart = mapper.readTree(response.body())
        assertEquals(1L, cart["customerId"].asLong())
        assertEquals("ACTIVE", cart["status"].asText())
        assertTrue(cart["items"].isArray)
        assertEquals(0, cart["items"].size())
    }

    @Test
    fun `POST cart items adds an item and returns 200 with the updated cart`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = post(port, "/customers/2/cart/items", addItemBody())

        assertEquals(200, response.statusCode())
        val cart = mapper.readTree(response.body())
        assertEquals(2L, cart["customerId"].asLong())
        assertEquals(1, cart["items"].size())
        assertEquals(10L, cart["items"][0]["productId"].asLong())
        assertEquals("Product 10", cart["items"][0]["productName"].asText())
        assertEquals(2, cart["items"][0]["quantity"].asInt())
        assertEquals("BRL", cart["items"][0]["currency"].asText())
    }

    @Test
    fun `POST cart items with same productId sums quantity and replaces the product summary`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/3/cart/items", addItemBody(quantity = 2))

        val response =
            post(
                port,
                "/customers/3/cart/items",
                addItemBody(productName = "Product 10 updated", unitPriceAmount = "24.90", quantity = 3),
            )

        assertEquals(200, response.statusCode())
        val cart = mapper.readTree(response.body())
        assertEquals(1, cart["items"].size())
        assertEquals(5, cart["items"][0]["quantity"].asInt())
        assertEquals("Product 10 updated", cart["items"][0]["productName"].asText())
        assertEquals(24.90, cart["items"][0]["unitPriceAmount"].asDouble())
    }

    @Test
    fun `POST cart items with quantity 0 returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = post(port, "/customers/4/cart/items", addItemBody(quantity = 0))

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/customers/4/cart/items",
        )
    }

    @Test
    fun `POST cart items with invalid currency returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = post(port, "/customers/5/cart/items", addItemBody(currency = "XYZ"))

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/customers/5/cart/items",
        )
    }

    @Test
    fun `DELETE cart item removes the item and returns 200 with the updated cart`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/6/cart/items", addItemBody(productId = 10L))
        post(port, "/customers/6/cart/items", addItemBody(productId = 20L))

        val response = delete(port, "/customers/6/cart/items/10")

        assertEquals(200, response.statusCode())
        val cart = mapper.readTree(response.body())
        assertEquals(1, cart["items"].size())
        assertEquals(20L, cart["items"][0]["productId"].asLong())
    }

    @Test
    fun `DELETE cart items clears the cart and returns 200 with an empty cart`() {
        val port = environment.getRequiredProperty("local.server.port")
        post(port, "/customers/7/cart/items", addItemBody(productId = 10L))
        post(port, "/customers/7/cart/items", addItemBody(productId = 20L))

        val response = delete(port, "/customers/7/cart/items")

        assertEquals(200, response.statusCode())
        val cart = mapper.readTree(response.body())
        assertEquals(0, cart["items"].size())
        assertEquals("ACTIVE", cart["status"].asText())
    }

    @Test
    fun `GET customer cart with non-existent customerId returns 400 problem details instead of 500`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "/customers/999999/cart")

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/customers/999999/cart",
        )
    }

    @Test
    fun `POST cart items with non-existent customerId returns 400 problem details instead of 500`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = post(port, "/customers/999999/cart/items", addItemBody())

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/customers/999999/cart/items",
        )
    }
}
