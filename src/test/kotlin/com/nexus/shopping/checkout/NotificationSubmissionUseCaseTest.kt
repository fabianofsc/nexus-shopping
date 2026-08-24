package com.nexus.shopping.checkout

import com.nexus.shopping.checkout.application.exception.CheckoutValidationException
import com.nexus.shopping.checkout.application.model.DiscardNotificationSubmissionCommand
import com.nexus.shopping.checkout.application.model.NotificationSubmission
import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import com.nexus.shopping.checkout.application.port.outbound.AcceptedNotification
import com.nexus.shopping.checkout.application.port.outbound.NotificationServiceClientPort
import com.nexus.shopping.checkout.application.port.outbound.NotificationServiceUnavailableException
import com.nexus.shopping.checkout.application.port.outbound.NotificationSubmissionRepositoryPort
import com.nexus.shopping.checkout.application.usecase.NotificationSubmissionUseCase
import com.nexus.shopping.platform.application.exception.ConflictException
import com.nexus.shopping.platform.application.exception.NotFoundException
import com.nexus.shopping.platform.domain.PageResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NotificationSubmissionUseCaseTest {
    private val repository = FakeNotificationSubmissionRepository()
    private val client = FakeNotificationServiceClient()
    private val useCase = NotificationSubmissionUseCase(repository, client)

    @Test
    fun `dispatch registra falha remota sanitizada e retorna normalmente`() {
        val pending = repository.save(pending())
        client.failure = NotificationServiceUnavailableException("timeout for secret@example.com")

        val result = useCase.dispatch(requireNotNull(pending.id))

        assertEquals(NotificationSubmissionStatus.FAILED, result.status)
        assertEquals(1, repository.attempts.single().attemptCount)
        assertEquals("Notification service is unavailable.", result.lastError)
    }

    @Test
    fun `retry reutiliza chave e payload persistidos`() {
        val failed = repository.save(pending(status = NotificationSubmissionStatus.FAILED))

        val result = useCase.retry(requireNotNull(failed.id))

        assertEquals(NotificationSubmissionStatus.ACCEPTED, result.status)
        assertEquals(failed.notificationKey, client.accepted.single().notificationKey)
        assertEquals(failed.body, client.accepted.single().body)
    }

    @Test
    fun `dispatch aceito conclui a submissao com identificador remoto`() {
        val pending = repository.save(pending())

        val result = useCase.dispatch(requireNotNull(pending.id))

        assertEquals(NotificationSubmissionStatus.ACCEPTED, result.status)
        assertEquals("ntf-1", result.notificationId)
        assertEquals(1, repository.attempts.single().attemptCount)
    }

    @Test
    fun `dispatch nao chama servico quando outra tentativa ja possui o claim`() {
        val pending = repository.save(pending())
        repository.claimAvailable = false

        val result = useCase.dispatch(requireNotNull(pending.id))

        assertEquals(NotificationSubmissionStatus.PENDING, result.status)
        assertEquals(emptyList(), client.accepted)
    }

    @Test
    fun `dispatch devolve estado atual quando perde o claim para outra tentativa`() {
        val pending = repository.save(pending())
        repository.claimAvailable = false
        repository.onClaimRejected = { repository.acceptByOtherWorker(requireNotNull(pending.id)) }

        val result = useCase.dispatch(requireNotNull(pending.id))

        assertEquals(NotificationSubmissionStatus.ACCEPTED, result.status)
        assertEquals("ntf-other-worker", result.notificationId)
        assertEquals(emptyList(), client.accepted)
    }

    @Test
    fun `dispatch de submissao ausente falha com validacao`() {
        assertFailsWith<NotFoundException> { useCase.dispatch(999) }
    }

    @Test
    fun `retry rejeita submissao terminal`() {
        val accepted = repository.save(pending(status = NotificationSubmissionStatus.ACCEPTED))

        assertFailsWith<ConflictException> { useCase.retry(requireNotNull(accepted.id)) }
        assertEquals(emptyList(), client.accepted)
    }

    @Test
    fun `retry recupera lease expirada e nao recupera lease ativa`() {
        val expired =
            repository.save(
                pending(
                    status = NotificationSubmissionStatus.IN_FLIGHT,
                    leaseUntil = Instant.now().minusSeconds(1),
                ),
            )
        val active =
            repository.save(
                pending(
                    status = NotificationSubmissionStatus.IN_FLIGHT,
                    leaseUntil = Instant.now().plusSeconds(30),
                ),
            )

        assertEquals(NotificationSubmissionStatus.ACCEPTED, useCase.retry(requireNotNull(expired.id)).status)
        assertFailsWith<ConflictException> { useCase.retry(requireNotNull(active.id)) }
        assertEquals(listOf(expired.notificationKey), client.accepted.map { it.notificationKey })
    }

    @Test
    fun `list rejeita pagina fora dos limites antes de consultar o repositorio`() {
        assertFailsWith<CheckoutValidationException> { useCase.list(null, -1, 50) }
        assertFailsWith<CheckoutValidationException> { useCase.list(null, 0, 0) }
        assertFailsWith<CheckoutValidationException> { useCase.list(null, 0, 501) }
        assertEquals(0, repository.findPageCalls)
    }

    @Test
    fun `retry de submissao inexistente retorna nao encontrado`() {
        assertFailsWith<NotFoundException> { useCase.retry(999) }
    }

    @Test
    fun `discard de submissao terminal retorna conflito`() {
        val accepted = repository.save(pending(status = NotificationSubmissionStatus.ACCEPTED))

        assertFailsWith<ConflictException> {
            useCase.discard(DiscardNotificationSubmissionCommand(requireNotNull(accepted.id), "operador confirmou cancelamento"))
        }
    }

    @Test
    fun `discard valida motivo e faz transicao terminal`() {
        val pending = repository.save(pending())

        assertFailsWith<CheckoutValidationException> {
            useCase.discard(DiscardNotificationSubmissionCommand(requireNotNull(pending.id), " "))
        }

        val result =
            useCase.discard(
                DiscardNotificationSubmissionCommand(
                    requireNotNull(pending.id),
                    "operador confirmou cancelamento",
                ),
            )

        assertEquals(NotificationSubmissionStatus.DISCARDED, result.status)
    }

    private fun pending(
        status: NotificationSubmissionStatus = NotificationSubmissionStatus.PENDING,
        leaseUntil: Instant? = null,
    ): NotificationSubmission =
        NotificationSubmission(
            orderId = 42,
            customerId = 7,
            attemptReference = "attempt-1",
            recipientEmail = "customer@example.com",
            notificationKey = "order-confirmed:42:attempt-1",
            referenceId = "order:42",
            subject = "Pedido 42 confirmado",
            body = "Seu pedido 42 foi confirmado.",
            status = status,
            sendingLeaseUntil = leaseUntil,
        )

    private class FakeNotificationServiceClient : NotificationServiceClientPort {
        var failure: RuntimeException? = null
        val accepted = mutableListOf<NotificationSubmission>()

        override fun accept(submission: NotificationSubmission): AcceptedNotification {
            failure?.let { throw it }
            accepted += submission
            return AcceptedNotification("ntf-1")
        }
    }

    private class FakeNotificationSubmissionRepository : NotificationSubmissionRepositoryPort {
        private val submissions = mutableMapOf<Long, NotificationSubmission>()
        val attempts = mutableListOf<NotificationSubmission>()
        var claimAvailable = true
        var onClaimRejected: (() -> Unit)? = null
        var findPageCalls = 0
        private var nextId = 1L

        fun save(submission: NotificationSubmission): NotificationSubmission {
            val persisted = submission.copy(id = nextId++)
            submissions[requireNotNull(persisted.id)] = persisted
            return persisted
        }

        override fun reserve(submission: NotificationSubmission): NotificationSubmission =
            submissions.values.firstOrNull { it.notificationKey == submission.notificationKey } ?: save(submission)

        override fun findById(submissionId: Long): NotificationSubmission? = submissions[submissionId]

        override fun claim(
            submissionId: Long,
            sendingLeaseToken: String,
            sendingLeaseUntil: Instant,
            now: Instant,
        ): NotificationSubmission? {
            val current = submissions[submissionId] ?: return null
            if (
                !claimAvailable || !current.isClaimable(now)
            ) {
                onClaimRejected?.invoke()
                return null
            }
            return current
                .copy(
                    status = NotificationSubmissionStatus.IN_FLIGHT,
                    attemptCount = current.attemptCount + 1,
                    sendingLeaseToken = sendingLeaseToken,
                    sendingLeaseUntil = sendingLeaseUntil,
                ).also {
                    submissions[submissionId] = it
                    attempts += it
                }
        }

        override fun markAccepted(
            submissionId: Long,
            sendingLeaseToken: String,
            notificationId: String,
            now: Instant,
        ): NotificationSubmission? =
            complete(
                submissionId,
                sendingLeaseToken,
                NotificationSubmissionStatus.ACCEPTED,
                notificationId,
                null,
            )

        override fun markFailed(
            submissionId: Long,
            sendingLeaseToken: String,
            lastError: String,
            now: Instant,
        ): NotificationSubmission? =
            complete(
                submissionId,
                sendingLeaseToken,
                NotificationSubmissionStatus.FAILED,
                null,
                lastError,
            )

        override fun discard(
            submissionId: Long,
            reason: String,
            now: Instant,
        ): NotificationSubmission? {
            val current = submissions[submissionId] ?: return null
            if (current.status !in setOf(NotificationSubmissionStatus.PENDING, NotificationSubmissionStatus.FAILED)) {
                return null
            }
            return current
                .copy(
                    status = NotificationSubmissionStatus.DISCARDED,
                    discardReason = reason,
                ).also {
                    submissions[submissionId] = it
                }
        }

        override fun findPage(
            status: NotificationSubmissionStatus?,
            page: Int,
            size: Int,
        ): PageResult<NotificationSubmission> {
            findPageCalls += 1
            return PageResult(
                submissions.values.filter { status == null || it.status == status },
                page,
                size,
                submissions.size,
                false,
            )
        }

        fun acceptByOtherWorker(submissionId: Long) {
            val current = requireNotNull(submissions[submissionId])
            submissions[submissionId] =
                current.copy(
                    status = NotificationSubmissionStatus.ACCEPTED,
                    notificationId = "ntf-other-worker",
                    sendingLeaseToken = null,
                    sendingLeaseUntil = null,
                )
        }

        private fun NotificationSubmission.isClaimable(now: Instant): Boolean =
            status in setOf(NotificationSubmissionStatus.PENDING, NotificationSubmissionStatus.FAILED) ||
                (status == NotificationSubmissionStatus.IN_FLIGHT && sendingLeaseUntil?.isBefore(now) == true)

        private fun complete(
            submissionId: Long,
            sendingLeaseToken: String,
            status: NotificationSubmissionStatus,
            notificationId: String?,
            lastError: String?,
        ): NotificationSubmission? {
            val current = submissions[submissionId] ?: return null
            if (current.sendingLeaseToken != sendingLeaseToken) return null
            return current
                .copy(
                    status = status,
                    notificationId = notificationId,
                    lastError = lastError,
                ).also {
                    submissions[submissionId] = it
                }
        }
    }
}
