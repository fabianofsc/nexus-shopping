package com.nexus.shopping.checkout.adapter.outbound.notification

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.nexus.shopping.checkout.application.model.NotificationSubmission
import com.nexus.shopping.checkout.application.port.outbound.NotificationServiceRejectedException
import com.nexus.shopping.checkout.application.port.outbound.NotificationServiceUnavailableException
import com.nexus.shopping.infra.http.ConfigurableRestClientFactory
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NotificationServiceHttpClientTest {
    private lateinit var wireMock: WireMockServer

    @BeforeTest
    fun startServer() {
        wireMock = WireMockServer(wireMockConfig().dynamicPort())
        wireMock.start()
    }

    @AfterTest
    fun stopServer() {
        wireMock.stop()
    }

    @Test
    fun `accept envia email completo com Basic Auth e chave idempotente`() {
        wireMock.stubFor(
            post(urlEqualTo("/v1/notifications"))
                .withBasicAuth("notification", "notification")
                .withHeader("Idempotency-Key", com.github.tomakehurst.wiremock.client.WireMock.equalTo("order-confirmed:42:attempt-1"))
                .withRequestBody(
                    equalToJson(
                        """{"channel":"EMAIL","recipient":{"email":"cliente@example.com"},"subject":"Pedido 42 confirmado","body":"Seu pedido 42 foi confirmado.","reference_id":"order:42","callback_id":"order:42","callback_name":"order_confirmed"}""",
                    ),
                )
                .willReturn(aResponse().withStatus(202).withBody("""{"notification_id":"ntf_1"}""")),
        )

        val accepted = client().accept(submission())

        assertEquals("ntf_1", accepted.notificationId)
    }

    @Test
    fun `accept trata timeout, limite e erro remoto como indisponibilidade`() {
        listOf(
            aResponse().withStatus(429),
            aResponse().withStatus(500),
            aResponse().withStatus(202).withFixedDelay(100),
        ).forEach { response ->
            wireMock.resetAll()
            wireMock.stubFor(post(urlEqualTo("/v1/notifications")).willReturn(response))

            assertFailsWith<NotificationServiceUnavailableException> { client(readTimeout = Duration.ofMillis(20)).accept(submission()) }
        }
    }

    @Test
    fun `accept trata rejeicoes remotas como nao recuperaveis`() {
        listOf(400, 401, 403, 409, 422).forEach { status ->
            wireMock.resetAll()
            wireMock.stubFor(post(urlEqualTo("/v1/notifications")).willReturn(aResponse().withStatus(status)))

            assertFailsWith<NotificationServiceRejectedException> { client().accept(submission()) }
        }
    }

    @Test
    fun `accept trata resposta aceita sem identificador valido como indisponibilidade`() {
        listOf("", "{}", "not-json").forEach { body ->
            wireMock.resetAll()
            wireMock.stubFor(post(urlEqualTo("/v1/notifications")).willReturn(aResponse().withStatus(202).withBody(body)))

            assertFailsWith<NotificationServiceUnavailableException> { client().accept(submission()) }
        }
    }

    @Test
    fun `accept exige o aceite remoto 202`() {
        wireMock.stubFor(
            post(urlEqualTo("/v1/notifications"))
                .willReturn(aResponse().withStatus(201).withBody("""{"notification_id":"ntf_1"}""")),
        )

        assertFailsWith<NotificationServiceUnavailableException> { client().accept(submission()) }
    }

    private fun client(readTimeout: Duration = Duration.ofSeconds(1)) =
        NotificationServiceHttpClient(
            ConfigurableRestClientFactory(),
            wireMock.baseUrl(),
            "notification",
            "notification",
            Duration.ofSeconds(1),
            readTimeout,
            jacksonObjectMapper(),
        )

    private fun submission() =
        NotificationSubmission(
            id = 10,
            orderId = 42,
            customerId = 7,
            attemptReference = "attempt-1",
            recipientEmail = "cliente@example.com",
            notificationKey = "order-confirmed:42:attempt-1",
            referenceId = "order:42",
            subject = "Pedido 42 confirmado",
            body = "Seu pedido 42 foi confirmado.",
        )
}
