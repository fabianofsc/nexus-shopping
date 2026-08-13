package com.nexus.shopping.payment.adapter.outbound.provider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.nexus.shopping.payment.application.exception.PaymentProviderGatewayException
import com.nexus.shopping.payment.application.port.outbound.PaymentProviderGateway
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingRequest
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingResult
import com.nexus.shopping.payment.application.port.outbound.ProviderStatusResult
import com.nexus.shopping.payment.domain.PaymentProvider
import com.nexus.shopping.payment.domain.PaymentStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body

@Component
@ConditionalOnProperty(prefix = "nexus.payment-service", name = ["enabled"], havingValue = "true")
class NexusPaymentServiceProviderGateway(
    restClientBuilder: RestClient.Builder,
    @Value("\${nexus.payment-service.base-url}")
    baseUrl: String,
) : PaymentProviderGateway {
    override val provider = PaymentProvider.NEXUS_PAYMENT_SERVICE

    private val restClient = restClientBuilder.baseUrl(baseUrl).build()
    private val errorMapper = ObjectMapper()

    override fun process(request: ProviderProcessingRequest): ProviderProcessingResult {
        val response =
            invoke {
                restClient
                    .post()
                    .uri("/v1/payments")
                    .header("Idempotency-Key", request.providerDispatchKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        DispatchRequestBody(
                            referenceId = request.referenceId,
                            amount =
                                request.amount.value
                                    .movePointRight(2)
                                    .longValueExact(),
                            paymentToken = request.paymentToken,
                        ),
                    ).retrieve()
                    .body<DispatchResponseBody>()
            }
        val dispatched = requireNotNull(response) { "nexus-payment-service returned an empty dispatch response." }
        return ProviderProcessingResult(
            status = PaymentStatus.REQUESTED,
            providerTransactionId = null,
            providerAttemptReference = dispatched.attemptReference,
        )
    }

    override fun checkStatus(providerAttemptReference: String): ProviderStatusResult {
        val response =
            invoke {
                restClient
                    .get()
                    .uri("/v1/payments/{attemptReference}", providerAttemptReference)
                    .retrieve()
                    .body<StatusResponseBody>()
            }
        val status = requireNotNull(response) { "nexus-payment-service returned an empty status response." }
        return ProviderStatusResult(
            status = status.status.toPaymentStatus(),
            providerTransactionId = null,
        )
    }

    private fun <T> invoke(call: () -> T): T =
        try {
            call()
        } catch (exception: RestClientResponseException) {
            throw PaymentProviderGatewayException(
                "nexus-payment-service returned ${exception.statusCode.value()} (${extractCode(exception)}).",
                exception,
            )
        }

    private fun extractCode(exception: RestClientResponseException): String =
        runCatching {
            errorMapper.readTree(exception.responseBodyAsString).get("code")?.asText()
        }.getOrNull() ?: "unknown"

    private fun String.toPaymentStatus(): PaymentStatus =
        when (this) {
            "APPROVED" -> PaymentStatus.APPROVED
            "REJECTED" -> PaymentStatus.REJECTED
            else -> PaymentStatus.REQUESTED
        }
}

private data class DispatchRequestBody(
    val referenceId: String,
    val amount: Long,
    val paymentToken: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class DispatchResponseBody(
    val attemptReference: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class StatusResponseBody(
    val attemptReference: String,
    val status: String,
)
