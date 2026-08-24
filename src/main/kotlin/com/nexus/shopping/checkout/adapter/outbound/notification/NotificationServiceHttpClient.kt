package com.nexus.shopping.checkout.adapter.outbound.notification

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.nexus.shopping.checkout.application.model.NotificationSubmission
import com.nexus.shopping.checkout.application.port.outbound.AcceptedNotification
import com.nexus.shopping.checkout.application.port.outbound.NotificationServiceClientPort
import com.nexus.shopping.checkout.application.port.outbound.NotificationServiceRejectedException
import com.nexus.shopping.checkout.application.port.outbound.NotificationServiceUnavailableException
import com.nexus.shopping.infra.http.ConfigurableRestClientFactory
import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.time.Duration
import kotlin.time.measureTime

@Component
class NotificationServiceHttpClient(
    factory: ConfigurableRestClientFactory,
    @Value("\${nexus.notification-service.base-url}") baseUrl: String,
    @param:Value("\${nexus.notification-service.username}") private val username: String,
    @param:Value("\${nexus.notification-service.password}") private val password: String,
    @Value("\${nexus.notification-service.connect-timeout}") connectTimeout: Duration,
    @Value("\${nexus.notification-service.read-timeout}") readTimeout: Duration,
    private val responseMapper: ObjectMapper,
) : NotificationServiceClientPort {
    private val restClient = factory.builder(connectTimeout, readTimeout).baseUrl(baseUrl).build()

    override fun accept(submission: NotificationSubmission): AcceptedNotification {
        try {
            lateinit var response: String
            val duration =
                measureTime {
                    val remoteResponse =
                        restClient
                            .post()
                            .uri("/v1/notifications")
                            .headers { headers -> headers.setBasicAuth(username, password) }
                            .header("Idempotency-Key", submission.notificationKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(submission.toRequestBody())
                            .retrieve()
                            .toEntity(String::class.java)
                    if (remoteResponse.statusCode.value() != 202) {
                        unavailable("unexpected_remote_status_${remoteResponse.statusCode.value()}", submission.id, null)
                    }
                    response = remoteResponse.body.orEmpty()
                }
            val notificationId = parseNotificationId(response)
            logger.infoWithContext(
                "notification_service.accepted",
                "notification.operation" to "accept",
                "notification.submission_id" to submission.id,
                "notification.remote_status" to 202,
                "notification.remote_id" to notificationId,
                "event.duration" to duration,
            )
            return AcceptedNotification(notificationId)
        } catch (exception: RestClientResponseException) {
            throw mapResponseException(exception, submission.id)
        } catch (exception: NotificationServiceUnavailableException) {
            throw exception
        } catch (exception: RestClientException) {
            unavailable("transport_failure", submission.id, exception)
        }
    }

    private fun parseNotificationId(response: String): String =
        try {
            responseMapper
                .readTree(response)
                .path("notification_id")
                .asText()
                .takeIf { it.isNotBlank() }
                ?: unavailable("invalid_accepted_response", null, null)
        } catch (_: Exception) {
            unavailable("invalid_accepted_response", null, null)
        }

    private fun mapResponseException(
        exception: RestClientResponseException,
        submissionId: Long?,
    ): RuntimeException {
        val status = exception.statusCode.value()
        return if (status == 429 || status >= 500) {
            unavailable("remote_status_$status", submissionId, exception)
        } else {
            logger.warnWithContext(
                "notification_service.rejected",
                "notification.operation" to "accept",
                "notification.submission_id" to submissionId,
                "notification.remote_status" to status,
            )
            NotificationServiceRejectedException("Notification service rejected the submission with HTTP $status.", exception)
        }
    }

    private fun unavailable(
        reason: String,
        submissionId: Long?,
        cause: Throwable?,
    ): Nothing {
        logger.warnWithContext(
            "notification_service.unavailable",
            "notification.operation" to "accept",
            "notification.submission_id" to submissionId,
            "notification.failure_reason" to reason,
        )
        throw NotificationServiceUnavailableException("Notification service is unavailable ($reason).", cause)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(NotificationServiceHttpClient::class.java)
    }
}

private fun NotificationSubmission.toRequestBody() =
    NotificationRequestBody(
        channel = "EMAIL",
        recipient = EmailRecipient(recipientEmail),
        subject = subject,
        body = body,
        referenceId = referenceId,
        callbackId = referenceId,
        callbackName = "order_confirmed",
    )

private data class NotificationRequestBody(
    val channel: String,
    val recipient: EmailRecipient,
    val subject: String,
    val body: String,
    @get:JsonProperty("reference_id")
    val referenceId: String,
    @get:JsonProperty("callback_id")
    val callbackId: String,
    @get:JsonProperty("callback_name")
    val callbackName: String,
)

private data class EmailRecipient(
    val email: String,
)
