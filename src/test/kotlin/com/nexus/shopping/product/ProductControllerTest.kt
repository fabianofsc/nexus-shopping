package com.nexus.shopping

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:nexus_shopping_controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class ProductControllerTest {
    @Autowired
    private lateinit var environment: Environment

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    private fun get(
        port: String,
        query: String,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/products$query"))
                .GET()
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun post(
        port: String,
        body: String,
        contentType: String = "application/json",
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/products"))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun patch(
        port: String,
        id: Long,
        body: String,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/products/$id"))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun delete(
        port: String,
        id: Long,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/products/$id"))
                .DELETE()
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun assertExceptionDetail(
        response: HttpResponse<String>,
        expectedStatus: Int,
        expectedTitle: String,
        expectedInstance: String,
        expectedDetail: String? = null,
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
        if (expectedDetail != null) {
            assertEquals(expectedDetail, exceptionDetail["detail"].asText())
        }
        return exceptionDetail
    }

    private fun assertNoInternalDetailsLeaked(body: String) {
        assertFalse(body.contains("DataIntegrityViolationException"))
        assertFalse(body.contains("JdbcSQLIntegrityConstraintViolationException"))
        assertFalse(body.contains("Referential integrity"))
        assertFalse(body.contains("ConstraintViolationException"))
        assertFalse(body.contains("org.hibernate"))
        assertFalse(body.contains("jakarta.persistence"))
        assertFalse(body.contains("constraint", ignoreCase = true))
        assertFalse(body.contains("org.springframework"))
        assertFalse(body.contains("java.sql"))
        assertFalse(body.contains("\tat "))
    }

    @Test
    fun `POST products returns 201 with created product`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "brandId": 1,
              "categoryId": 1,
              "sku": "SKU-CTRL-001",
              "name": "Controller Test Product",
              "slug": "controller-test-product",
              "priceAmount": 49.90
            }
            """.trimIndent()

        val response = post(port, body)

        assertEquals(201, response.statusCode())
        val product = mapper.readTree(response.body())
        assertNotNull(product["id"].asLong().takeIf { it > 0 }, "Expected a generated id > 0")
        assertEquals("SKU-CTRL-001", product["sku"].asText())
        assertEquals("Controller Test Product", product["name"].asText())
        assertEquals("ACTIVE", product["status"].asText())
        assertEquals("BRL", product["currency"].asText())
        assertEquals(0, product["inventoryQuantity"].asInt())
    }

    @Test
    fun `GET products with both categoryId and name returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "?categoryId=1&name=Product&page=0&size=50")

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "Use either categoryId or name, not both.",
        )
    }

    @Test
    fun `GET products with non-numeric categoryId returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "?categoryId=abc&page=0&size=50")

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "Invalid request.",
        )
        assertNoInternalDetailsLeaked(response.body())
    }

    @Test
    fun `POST products with blank sku returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "brandId": 1,
              "categoryId": 1,
              "sku": "",
              "name": "Some Product",
              "slug": "some-product",
              "priceAmount": 10.00
            }
            """.trimIndent()

        val response = post(port, body)

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "sku must not be blank.",
        )
    }

    @Test
    fun `POST products with negative price returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "brandId": 1,
              "categoryId": 1,
              "sku": "SKU-NEG",
              "name": "Negative Price",
              "slug": "negative-price",
              "priceAmount": -1.00
            }
            """.trimIndent()

        val response = post(port, body)

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "priceAmount must be >= 0.",
        )
    }

    @Test
    fun `POST products with invalid status returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "brandId": 1,
              "categoryId": 1,
              "sku": "SKU-STATUS",
              "name": "Status Product",
              "slug": "status-product",
              "priceAmount": 10.00,
              "status": "DELETED"
            }
            """.trimIndent()

        val response = post(port, body)

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "status must be one of: ACTIVE, INACTIVE, ARCHIVED.",
        )
    }

    @Test
    fun `POST products with malformed JSON returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = post(port, """{"brandId":""")

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "Malformed request body.",
        )
    }

    @Test
    fun `POST products with missing foreign key returns 500 generic problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "brandId": 999999999,
              "categoryId": 1,
              "sku": "SKU-FK-FAIL",
              "name": "Foreign Key Failure",
              "slug": "foreign-key-failure",
              "priceAmount": 10.00
            }
            """.trimIndent()

        val response = post(port, body)

        assertExceptionDetail(
            response = response,
            expectedStatus = 500,
            expectedTitle = "Internal Server Error",
            expectedInstance = "/products",
            expectedDetail = "Unexpected server error.",
        )
        assertNoInternalDetailsLeaked(response.body())
    }

    @Test
    fun `POST products with unsupported media type returns 415 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = post(port, "plain text", contentType = "text/plain")

        assertExceptionDetail(
            response = response,
            expectedStatus = 415,
            expectedTitle = "Unsupported Media Type",
            expectedInstance = "/products",
            expectedDetail = "Content type is not supported.",
        )
    }

    @Test
    fun `PATCH products updates price and returns 200 with full product`() {
        val port = environment.getRequiredProperty("local.server.port")
        val response = patch(port, 1L, """{"priceAmount": 99.90}""")

        assertEquals(200, response.statusCode())
        val product = mapper.readTree(response.body())
        assertEquals(1L, product["id"].asLong())
        assertEquals(0, BigDecimal("99.90").compareTo(BigDecimal(product["priceAmount"].asText())))
    }

    @Test
    fun `PATCH products with priceAmount zero returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val response = patch(port, 1L, """{"priceAmount": 0}""")

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products/1",
            expectedDetail = "priceAmount must be greater than zero.",
        )
    }

    @Test
    fun `PATCH products with non-existent id returns 404 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val response = patch(port, 9999999999L, """{"priceAmount": 99.90}""")

        assertExceptionDetail(
            response = response,
            expectedStatus = 404,
            expectedTitle = "Not Found",
            expectedInstance = "/products/9999999999",
            expectedDetail = "Product 9999999999 not found.",
        )
    }

    @Test
    fun `DELETE products returns 405 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val response = delete(port, 1L)

        assertExceptionDetail(
            response = response,
            expectedStatus = 405,
            expectedTitle = "Method Not Allowed",
            expectedInstance = "/products/1",
            expectedDetail = "Request method is not supported.",
        )
        val allow = response.headers().firstValue("Allow").orElse("")
        assertTrue(allow.split(",").map { it.trim() }.contains("PATCH"))
    }

    @Test
    fun `GET products with page below zero returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "?categoryId=1&page=-1&size=50")

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "Query parameter page must be greater than or equal to 0.",
        )
    }

    @Test
    fun `GET products with size above limit returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "?categoryId=1&page=0&size=999")

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "Query parameter size must be between 1 and 500.",
        )
    }

    @Test
    fun `GET products with no search parameter returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "?page=0&size=50")

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "Query parameter categoryId or name is required.",
        )
    }
}
