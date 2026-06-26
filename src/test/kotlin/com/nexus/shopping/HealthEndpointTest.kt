package com.nexus.shopping

import com.fasterxml.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:nexus_shopping_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.endpoint.health.show-details=always",
    ],
)
class HealthEndpointTest {

    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `should expose actuator health endpoint with database status`() {
        val port = environment.getRequiredProperty("local.server.port")
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/actuator/health"))
            .GET()
            .build()

        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode())
        val health = JsonMapper.builder().build().readTree(response.body())
        assertEquals("UP", health["status"].asText())
        assertEquals("UP", health["components"]["db"]["status"].asText())
        assertEquals(3, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Int::class.java))
    }

    @Test
    fun `should expose products by category using the indexed read query`() {
        val port = environment.getRequiredProperty("local.server.port")
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/products?categoryId=1"))
            .GET()
            .build()

        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode())
        val products = JsonMapper.builder().build().readTree(response.body())
        assertEquals(1, products.size())
        assertEquals("SKU-1", products.first()["sku"].asText())
    }

    @Test
    fun `should expose products by name using the indexed prefix like query`() {
        val port = environment.getRequiredProperty("local.server.port")
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/products?name=Product%201"))
            .GET()
            .build()

        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode())
        val products = JsonMapper.builder().build().readTree(response.body())
        assertEquals(1, products.size())
        assertEquals("Product 1", products.first()["name"].asText())
    }
}
