package com.nexus.shopping.shared.observability

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:nexus_shopping_correlation_id_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false",
    ],
)
class CorrelationIdFilterTest {

    @Autowired
    private lateinit var environment: Environment

    private val httpClient = HttpClient.newHttpClient()

    @BeforeEach
    fun setUp() {
        MDC.clear()
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    private fun getRequest(path: String, correlationId: String? = null): HttpResponse<String> {
        val port = environment.getProperty("local.server.port")!!
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .GET()

        if (correlationId != null) {
            requestBuilder.header("X-Correlation-ID", correlationId)
        }

        val request = requestBuilder.build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    @Test
    fun `header ausente gera UUID e devolve na resposta`() {
        val response = getRequest("/test")

        assertEquals(200, response.statusCode())
        val responseHeader = response.headers().firstValue("X-Correlation-ID")
        assertNotNull(responseHeader)
        assertEquals(36, responseHeader.get().length) // Formato UUID v4
    }

    @Test
    fun `header válido é preservado e devolvido`() {
        val valid = "trace-001-abc"
        val response = getRequest("/test", valid)

        assertEquals(200, response.statusCode())
        val responseHeader = response.headers().firstValue("X-Correlation-ID")
        assertNotNull(responseHeader)
        assertEquals(valid, responseHeader.get())
    }

    @Test
    fun `header inválido gera UUID`() {
        val response = getRequest("/test", "trace@injection")

        assertEquals(200, response.statusCode())
        val responseHeader = response.headers().firstValue("X-Correlation-ID")
        assertNotNull(responseHeader)
        assertEquals(36, responseHeader.get().length) // UUID v4
    }

    @Test
    fun `header oversized gera UUID`() {
        val oversized = "x".repeat(129)
        val response = getRequest("/test", oversized)

        assertEquals(200, response.statusCode())
        val responseHeader = response.headers().firstValue("X-Correlation-ID")
        assertNotNull(responseHeader)
        assertEquals(36, responseHeader.get().length)
    }

    @Test
    fun `MDC é limpo após a requisição`() {
        getRequest("/test", "trace-001")

        // Após a resposta, MDC deve estar vazio
        assertNull(MDC.get("correlation.id"))
    }
}

@RestController
class TestController {
    @GetMapping("/test")
    fun test() = "ok"
}
