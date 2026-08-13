package com.nexus.shopping.payment.adapter.outbound.provider

import com.nexus.shopping.payment.application.exception.PaymentProviderGatewayException
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingRequest
import com.nexus.shopping.payment.domain.PaymentAmount
import com.nexus.shopping.payment.domain.PaymentCurrency
import com.nexus.shopping.payment.domain.PaymentStatus
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NexusPaymentServiceProviderGatewayTest {
    @Test
    fun `process sends the derived idempotency key and body then maps 202 PROCESSING to a requested result`() {
        val (gateway, server) = gatewayWithMockServer()
        server
            .expect(requestTo("http://nexus-payment-service/v1/payments"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(header("Idempotency-Key", "dispatch-key-1"))
            .andExpect(content().json("""{"referenceId":"checkout:42","amount":1990,"paymentToken":"card_processing_approved"}"""))
            .andRespond(
                withStatus(HttpStatus.ACCEPTED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        """{"attemptReference":"nexus-attempt-1","referenceId":"checkout:42","status":"PROCESSING","replayed":false}""",
                    ),
            )

        val result = gateway.process(request())

        assertEquals(PaymentStatus.REQUESTED, result.status)
        assertEquals("nexus-attempt-1", result.providerAttemptReference)
        assertNull(result.providerTransactionId)
        server.verify()
    }

    @Test
    fun `checkStatus maps a still processing remote status to a non terminal payment status`() {
        val (gateway, server) = gatewayWithMockServer()
        server
            .expect(requestTo("http://nexus-payment-service/v1/payments/nexus-attempt-1"))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """{"attemptReference":"nexus-attempt-1","status":"PROCESSING"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = gateway.checkStatus("nexus-attempt-1")

        assertEquals(PaymentStatus.REQUESTED, result.status)
        server.verify()
    }

    @Test
    fun `checkStatus maps an approved remote status to a terminal payment status`() {
        val (gateway, server) = gatewayWithMockServer()
        server
            .expect(requestTo("http://nexus-payment-service/v1/payments/nexus-attempt-1"))
            .andRespond(
                withSuccess(
                    """{"attemptReference":"nexus-attempt-1","status":"APPROVED"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = gateway.checkStatus("nexus-attempt-1")

        assertEquals(PaymentStatus.APPROVED, result.status)
        server.verify()
    }

    @Test
    fun `checkStatus maps a rejected remote status to a terminal payment status`() {
        val (gateway, server) = gatewayWithMockServer()
        server
            .expect(requestTo("http://nexus-payment-service/v1/payments/nexus-attempt-1"))
            .andRespond(
                withSuccess(
                    """{"attemptReference":"nexus-attempt-1","status":"REJECTED"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = gateway.checkStatus("nexus-attempt-1")

        assertEquals(PaymentStatus.REJECTED, result.status)
        server.verify()
    }

    @Test
    fun `an unprocessable dispatch response is mapped to a gateway exception preserving the error code`() {
        val (gateway, server) = gatewayWithMockServer()
        server
            .expect(requestTo("http://nexus-payment-service/v1/payments"))
            .andRespond(
                withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"code":"invalid_amount","message":"amount must be a positive integer of cents"}"""),
            )

        val exception =
            assertFailsWith<PaymentProviderGatewayException> {
                gateway.process(request())
            }

        assertContains(exception.message.orEmpty(), "invalid_amount")
    }

    @Test
    fun `a server error on status check is mapped to a gateway exception`() {
        val (gateway, server) = gatewayWithMockServer()
        server
            .expect(requestTo("http://nexus-payment-service/v1/payments/nexus-attempt-1"))
            .andRespond(
                withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"code":"internal_error","message":"boom"}"""),
            )

        val exception =
            assertFailsWith<PaymentProviderGatewayException> {
                gateway.checkStatus("nexus-attempt-1")
            }

        assertContains(exception.message.orEmpty(), "internal_error")
    }

    private fun request() =
        ProviderProcessingRequest(
            referenceId = "checkout:42",
            amount = PaymentAmount.of("19.90".toBigDecimal()),
            currency = PaymentCurrency.of("BRL"),
            paymentToken = "card_processing_approved",
            providerDispatchKey = "dispatch-key-1",
        )

    private fun gatewayWithMockServer(): Pair<NexusPaymentServiceProviderGateway, MockRestServiceServer> {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val gateway = NexusPaymentServiceProviderGateway(builder, "http://nexus-payment-service")
        return gateway to server
    }
}
