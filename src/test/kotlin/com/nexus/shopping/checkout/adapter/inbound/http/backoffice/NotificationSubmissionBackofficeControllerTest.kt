package com.nexus.shopping.checkout.adapter.inbound.http.backoffice

import com.nexus.shopping.checkout.application.model.DiscardNotificationSubmissionCommand
import com.nexus.shopping.checkout.application.model.NotificationSubmission
import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import com.nexus.shopping.checkout.application.model.NotificationSubmissionSummary
import com.nexus.shopping.checkout.application.port.inbound.NotificationSubmissionBackofficeInputPort
import com.nexus.shopping.checkout.application.port.outbound.AcceptedNotification
import com.nexus.shopping.checkout.application.port.outbound.NotificationServiceClientPort
import com.nexus.shopping.checkout.application.port.outbound.NotificationSubmissionRepositoryPort
import com.nexus.shopping.checkout.application.usecase.NotificationSubmissionUseCase
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
            .andExpect(jsonPath("$.content[0].orderId").value(42))
            .andExpect(jsonPath("$.content[0].notificationKey").value("order-confirmed:42:attempt-1"))
            .andExpect(jsonPath("$.content[0].status").value("FAILED"))
            .andExpect(jsonPath("$.content[0].lastError").value("timeout"))
            .andExpect(jsonPath("$.content[0].recipientEmail").doesNotExist())
            .andExpect(jsonPath("$.content[0].body").doesNotExist())
            .andExpect(jsonPath("$.content[0].discardReason").doesNotExist())

        check(inputPort.listArguments == Triple(NotificationSubmissionStatus.FAILED, 0, 50))
    }

    @Test
    fun `POST retry retorna submissão atualizada`() {
        inputPort.retryResult = summary(status = NotificationSubmissionStatus.ACCEPTED, notificationId = "ntf_1")

        client
            .perform(post("/backoffice/notification-submissions/10/retry"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.orderId").value(42))
            .andExpect(jsonPath("$.notificationKey").value("order-confirmed:42:attempt-1"))
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
        realClient()
            .perform(get("/backoffice/notification-submissions?page=-1&size=50"))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.instance").value("/backoffice/notification-submissions"))
    }

    @Test
    fun `POST discard com JSON malformado retorna problem details`() {
        client
            .perform(
                post("/backoffice/notification-submissions/10/discard")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":"),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.instance").value("/backoffice/notification-submissions/10/discard"))
    }

    @Test
    fun `POST retry de ID inexistente retorna problem details 404`() {
        realClient()
            .perform(post("/backoffice/notification-submissions/999/retry"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.title").value("Not Found"))
            .andExpect(jsonPath("$.instance").value("/backoffice/notification-submissions/999/retry"))
    }

    @Test
    fun `POST retry de submissao terminal retorna problem details 409`() {
        realClient(acceptedSubmission())
            .perform(post("/backoffice/notification-submissions/10/retry"))
            .andExpect(status().isConflict)
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.title").value("Conflict"))
            .andExpect(jsonPath("$.instance").value("/backoffice/notification-submissions/10/retry"))
    }

    private fun realClient(vararg submissions: NotificationSubmission): MockMvc =
        MockMvcBuilders
            .standaloneSetup(
                NotificationSubmissionBackofficeController(
                    NotificationSubmissionUseCase(
                        BackofficeRepository(submissions.associateBy { requireNotNull(it.id) }),
                        UnusedNotificationClient(),
                    ),
                ),
            ).setControllerAdvice(
                ApiExceptionHandler(),
            )
            .build()

    private fun acceptedSubmission(): NotificationSubmission =
        NotificationSubmission(
            id = 10L,
            orderId = 42L,
            customerId = 7L,
            attemptReference = "attempt-1",
            recipientEmail = "customer@example.com",
            notificationKey = "order-confirmed:42:attempt-1",
            referenceId = "order:42",
            subject = "Pedido 42 confirmado",
            body = "Seu pedido 42 foi confirmado.",
            status = NotificationSubmissionStatus.ACCEPTED,
        )

    private fun summary(
        status: NotificationSubmissionStatus,
        lastError: String? = null,
        notificationId: String? = null,
    ) = NotificationSubmissionSummary(
        id = 10L,
        orderId = 42L,
        notificationKey = "order-confirmed:42:attempt-1",
        status = status,
        attemptCount = 2,
        lastError = lastError,
        notificationId = notificationId,
        createdAt = Instant.parse("2026-08-20T10:00:00Z"),
        updatedAt = Instant.parse("2026-08-20T10:01:00Z"),
    )

    private class FakeNotificationSubmissionBackofficeInputPort : NotificationSubmissionBackofficeInputPort {
        var page: PageResult<NotificationSubmissionSummary> = PageResult(emptyList(), 0, 50, 0, false)
        var retryResult: NotificationSubmissionSummary? = null
        var discardResult: NotificationSubmissionSummary? = null
        var listArguments: Triple<NotificationSubmissionStatus?, Int, Int>? = null
        var retrySubmissionId: Long? = null
        var discardCommand: DiscardNotificationSubmissionCommand? = null

        override fun list(
            status: NotificationSubmissionStatus?,
            page: Int,
            size: Int,
        ): PageResult<NotificationSubmissionSummary> {
            listArguments = Triple(status, page, size)
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

    private class BackofficeRepository(
        private val submissions: Map<Long, NotificationSubmission>,
    ) : NotificationSubmissionRepositoryPort {
        override fun reserve(submission: NotificationSubmission): NotificationSubmission = error("not used")

        override fun findById(submissionId: Long): NotificationSubmission? = submissions[submissionId]

        override fun claim(
            submissionId: Long,
            sendingLeaseToken: String,
            sendingLeaseUntil: Instant,
            now: Instant,
        ): NotificationSubmission? = error("not used")

        override fun markAccepted(
            submissionId: Long,
            sendingLeaseToken: String,
            notificationId: String,
            now: Instant,
        ): NotificationSubmission? = error("not used")

        override fun markFailed(
            submissionId: Long,
            sendingLeaseToken: String,
            lastError: String,
            now: Instant,
        ): NotificationSubmission? = error("not used")

        override fun discard(
            submissionId: Long,
            reason: String,
            now: Instant,
        ): NotificationSubmission? = error("not used")

        override fun findPage(
            status: NotificationSubmissionStatus?,
            page: Int,
            size: Int,
        ): PageResult<NotificationSubmission> = error("not used")
    }

    private class UnusedNotificationClient : NotificationServiceClientPort {
        override fun accept(submission: NotificationSubmission): AcceptedNotification = error("not used")
    }
}
