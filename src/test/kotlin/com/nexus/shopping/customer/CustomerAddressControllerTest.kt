package com.nexus.shopping.customer

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
        "spring.datasource.url=jdbc:h2:mem:nexus_shopping_customer_address_controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class CustomerAddressControllerTest : RedisIntegrationTest() {
    @Autowired
    private lateinit var environment: Environment

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    @Test
    fun `GET customer address returns the registered address`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)

        val response = getAddress(port, customerId)

        assertEquals(200, response.statusCode())
        val address = mapper.readTree(response.body())
        assertEquals("Rua das Flores", address["street"].asText())
        assertEquals("Sao Paulo", address["city"].asText())
    }

    @Test
    fun `PUT customer address overwrites the registered address`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)

        val response = putAddress(port, customerId, addressBody("Av. Paulista"))

        assertEquals(200, response.statusCode())
        val address = mapper.readTree(response.body())
        assertEquals("Av. Paulista", address["street"].asText())

        val after = mapper.readTree(getAddress(port, customerId).body())
        assertEquals("Av. Paulista", after["street"].asText())
    }

    @Test
    fun `PUT customer address for a non-existent customer returns 404 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = putAddress(port, 9_999_999_999L, addressBody("Av. Paulista"))

        assertEquals(404, response.statusCode())
        assertTrue(
            response
                .headers()
                .firstValue("Content-Type")
                .orElse("")
                .startsWith("application/problem+json"),
        )
    }

    @Test
    fun `PUT customer address with blank city returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)

        val response = putAddress(port, customerId, addressBody(city = ""))

        assertEquals(400, response.statusCode())
    }

    @Test
    fun `GET customer address for a non-existent customer returns 404 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = getAddress(port, 9_999_999_999L)

        assertEquals(404, response.statusCode())
        assertTrue(
            response
                .headers()
                .firstValue("Content-Type")
                .orElse("")
                .startsWith("application/problem+json"),
        )
    }

    private fun createCustomer(port: String): Long {
        val body =
            """
            {
              "name": "Ana Silva",
              "document": "02648629025",
              "documentType": "CPF",
              "email": "ana.silva@example.com",
              "street": "Rua das Flores",
              "number": "123",
              "neighborhood": "Centro",
              "city": "Sao Paulo",
              "state": "SP",
              "zipCode": "01001000",
              "country": "BR"
            }
            """.trimIndent()
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/customers"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return mapper.readTree(response.body())["id"].asLong()
    }

    private fun addressBody(
        street: String = "Av. Paulista",
        city: String = "Sao Paulo",
    ): String =
        """
        {
          "street": "$street",
          "number": "1000",
          "neighborhood": "Bela Vista",
          "city": "$city",
          "state": "SP",
          "zipCode": "01310000",
          "country": "BR"
        }
        """.trimIndent()

    private fun getAddress(
        port: String,
        customerId: Long,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/customers/$customerId/address"))
                .GET()
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun putAddress(
        port: String,
        customerId: Long,
        body: String,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/customers/$customerId/address"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
