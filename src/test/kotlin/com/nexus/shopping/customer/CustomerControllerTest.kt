package com.nexus.shopping.customer

import com.fasterxml.jackson.databind.JsonNode
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:nexus_shopping_customer_controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class CustomerControllerTest : RedisIntegrationTest() {
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
                .uri(URI.create("http://localhost:$port/customers"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun getById(
        port: String,
        id: Long,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/customers/$id"))
                .GET()
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun assertExceptionDetail(
        response: HttpResponse<String>,
        expectedStatus: Int,
        expectedTitle: String,
        expectedInstance: String,
        expectedDetail: String,
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
        assertEquals(expectedDetail, exceptionDetail["detail"].asText())
        return exceptionDetail
    }

    @Test
    fun `POST customers returns 201 with created customer`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "name": "Ana Silva",
              "document": "02648629025",
              "documentType": "CPF",
              "email": "ana.silva@example.com",
              "phone": "+5511999990000",
              "street": "Rua das Flores",
              "number": "123",
              "complement": "Apto 45",
              "neighborhood": "Centro",
              "city": "Sao Paulo",
              "state": "SP",
              "zipCode": "01001000",
              "country": "BR"
            }
            """.trimIndent()

        val response = post(port, body)

        assertEquals(201, response.statusCode())
        val customer = mapper.readTree(response.body())
        assertNotNull(customer["id"].asLong().takeIf { it > 0 }, "Expected a generated id > 0")
        assertEquals("Ana Silva", customer["name"].asText())
        assertEquals("02648629025", customer["document"].asText())
        assertEquals("CPF", customer["documentType"].asText())
        assertEquals("ACTIVE", customer["status"].asText())
        assertEquals("ana.silva@example.com", customer["contact"]["email"].asText())
        assertEquals("+5511999990000", customer["contact"]["phone"].asText())
        assertEquals("Sao Paulo", customer["address"]["city"].asText())
        assertEquals("BR", customer["address"]["country"].asText())
    }

    @Test
    fun `GET customer by id returns 200 with customer`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "name": "Bruno Costa",
              "document": "18184222230",
              "documentType": "CNH",
              "email": "bruno.costa@example.com",
              "street": "Avenida Central",
              "number": "500",
              "neighborhood": "Bela Vista",
              "city": "Rio de Janeiro",
              "state": "RJ",
              "zipCode": "20040002",
              "country": "BR"
            }
            """.trimIndent()
        val created = mapper.readTree(post(port, body).body())

        val response = getById(port, created["id"].asLong())

        assertEquals(200, response.statusCode())
        val customer = mapper.readTree(response.body())
        assertEquals(created["id"].asLong(), customer["id"].asLong())
        assertEquals("Bruno Costa", customer["name"].asText())
        assertEquals("CNH", customer["documentType"].asText())
        assertEquals("bruno.costa@example.com", customer["contact"]["email"].asText())
        assertEquals("Rio de Janeiro", customer["address"]["city"].asText())
    }

    @Test
    fun `GET customer by id with non-existent id returns 404 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = getById(port, 9999999999L)

        assertExceptionDetail(
            response = response,
            expectedStatus = 404,
            expectedTitle = "Not Found",
            expectedInstance = "/customers/9999999999",
            expectedDetail = "Customer 9999999999 not found.",
        )
    }

    @Test
    fun `POST customers with blank name returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "name": "",
              "document": "02648629025",
              "documentType": "CPF",
              "email": "invalid@example.com",
              "street": "Rua das Flores",
              "number": "123",
              "neighborhood": "Centro",
              "city": "Sao Paulo",
              "state": "SP",
              "zipCode": "01001000",
              "country": "BR"
            }
            """.trimIndent()

        val response = post(port, body)

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/customers",
            expectedDetail = "name must not be blank.",
        )
    }
}
