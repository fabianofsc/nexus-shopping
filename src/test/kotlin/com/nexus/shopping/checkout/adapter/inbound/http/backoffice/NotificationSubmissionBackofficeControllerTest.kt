package com.nexus.shopping.checkout.adapter.inbound.http.backoffice

import com.nexus.shopping.checkout.application.exception.CheckoutValidationException
import com.nexus.shopping.checkout.application.model.DiscardNotificationSubmissionCommand
import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import com.nexus.shopping.checkout.application.model.NotificationSubmissionSummary
import com.nexus.shopping.checkout.application.port.inbound.NotificationSubmissionBackofficeInputPort
import com.nexus.shopping.platform.adapter.inbound.http.ApiExceptionHandler
import com.nexus.shopping.platform.domain.PageResult
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import kotlin.test.Test

class NotificationSubmissionBackofficeControllerTest {
    private val inputPort = FakeNotificationSubmissionBackofficeInputPort()
    private val client: MockMvc =
        MockMvcBuilders
            .standaloneSetup(NotificationSubmissionBackofficeController(inputPort))
            .setControllerAdvice(ApiExceptionHandler())
            .build()

    @Test
    fun `GET lista submissões sem destinatário ou corpo`() {
        inputPort.page =
            PageResult(
                content = listOf(summary(status = NotificationSubmissionStatus.FAILED, lastError = "timeout")),
                page = 0,
                size = 50,
                count = 1,
                hasNext = false,
            )

        client
            .perform(get("/backoffice/notification-submissions?status=FAILED&page=0&size=50"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].id").value(10))
            .andExpect(jsonPath("$.content[0].status").value("FAILED"))
            .andExpect(jsonPath("$.content[0].lastError").value("timeout"))
            .andExpect(jsonPath("$.content[0].recipientEmail").doesNotExist())
            .andExpect(jsonPath("$.content[0].body").doesNotExist())

        check(inputPort.listArguments == Triple(NotificationSubmissionStatus.FAILED, 0, 50))
    }

    @Test
    fun `POST retry retorna submissão atualizada`() {
        inputPort.retryResult = summary(status = NotificationSubmissionStatus.ACCEPTED, notificationId = "ntf_1")

        client
            .perform(post("/backoffice/notification-submissions/10/retry"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.notificationId").value("ntf_1"))

        check(inputPort.retrySubmissionId == 10L)
    }

    @Test
    fun `POST discard converte request em command`() {
        inputPort.discardResult = summary(status = NotificationSubmissionStatus.DISCARDED)

        client
            .perform(
                post("/backoffice/notification-submissions/10/discard")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reason":"template remoto invalido"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("DISCARDED"))

        check(inputPort.discardCommand == DiscardNotificationSubmissionCommand(10L, "template remoto invalido"))
    }

    @Test
    fun `GET com pagina invalida retorna problem details`() {
        inputPort.listFailure = CheckoutValidationException("page must be greater than or equal to zero.")

        client
            .perform(get("/backoffice/notification-submissions?page=-1&size=50"))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.instance").value("/backoffice/notification-submissions"))
    }

    private fun summary(
        status: NotificationSubmissionStatus,
        lastError: String? = null,
        notificationId: String? = null,
    ) = NotificationSubmissionSummary(
        id = 10L,
        status = status,
        attemptCount = 2,
        lastError = lastError,
        notificationId = notificationId,
        discardReason = null,
        createdAt = Instant.parse("2026-08-20T10:00:00Z"),
        updatedAt = Instant.parse("2026-08-20T10:01:00Z"),
    )

    private class FakeNotificationSubmissionBackofficeInputPort : NotificationSubmissionBackofficeInputPort {
        var page: PageResult<NotificationSubmissionSummary> = PageResult(emptyList(), 0, 50, 0, false)
        var retryResult: NotificationSubmissionSummary? = null
        var discardResult: NotificationSubmissionSummary? = null
        var listFailure: RuntimeException? = null
        var listArguments: Triple<NotificationSubmissionStatus?, Int, Int>? = null
        var retrySubmissionId: Long? = null
        var discardCommand: DiscardNotificationSubmissionCommand? = null

        override fun list(
            status: NotificationSubmissionStatus?,
            page: Int,
            size: Int,
        ): PageResult<NotificationSubmissionSummary> {
            listArguments = Triple(status, page, size)
            listFailure?.let { throw it }
            return this.page
        }

        override fun retry(submissionId: Long): NotificationSubmissionSummary {
            retrySubmissionId = submissionId
            return checkNotNull(retryResult) { "retry result was not configured" }
        }

        override fun discard(command: DiscardNotificationSubmissionCommand): NotificationSubmissionSummary {
            discardCommand = command
            return checkNotNull(discardResult) { "discard result was not configured" }
        }
    }
}
