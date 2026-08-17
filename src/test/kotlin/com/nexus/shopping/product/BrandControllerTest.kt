package com.nexus.shopping.product

import com.fasterxml.jackson.databind.json.JsonMapper
import com.nexus.shopping.support.RedisIntegrationTest
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
        "spring.datasource.url=jdbc:h2:mem:brand_controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class BrandControllerTest : RedisIntegrationTest() {
    @Autowired
    private lateinit var environment: Environment

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    @Test
    fun `GET brands returns the seeded brands`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "/brands")

        assertEquals(200, response.statusCode())
        assertTrue(mapper.readTree(response.body()).size() > 0)
    }

    @Test
    fun `POST brands creates a brand and returns 201`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = post(port, "/brands", """{"name":"Apple","description":"Tech brand"}""")

        assertEquals(201, response.statusCode())
        val brand = mapper.readTree(response.body())
        assertEquals("Apple", brand["name"].asText())
        assertEquals("Tech brand", brand["description"].asText())
    }

    @Test
    fun `POST brands with blank name returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = post(port, "/brands", """{"name":""}""")

        assertEquals(400, response.statusCode())
        assertTrue(
            response
                .headers()
                .firstValue("Content-Type")
                .orElse("")
                .startsWith("application/problem+json"),
        )
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
}
