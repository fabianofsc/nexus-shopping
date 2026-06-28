package com.nexus.shopping

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment

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

    private fun post(port: String, body: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/products"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    @Test
    fun `POST products returns 201 with created product`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body = """
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
    fun `POST products with blank sku returns 400`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body = """
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

        assertEquals(400, response.statusCode())
    }

    @Test
    fun `POST products with negative price returns 400`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body = """
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

        assertEquals(400, response.statusCode())
    }

    @Test
    fun `POST products with invalid status returns 400`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body = """
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

        assertEquals(400, response.statusCode())
    }
}
