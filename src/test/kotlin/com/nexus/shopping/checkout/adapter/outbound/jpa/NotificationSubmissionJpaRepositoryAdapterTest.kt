package com.nexus.shopping.checkout.adapter.outbound.jpa

import com.nexus.shopping.checkout.application.model.NotificationSubmission
import com.nexus.shopping.checkout.application.model.NotificationSubmissionStatus
import com.nexus.shopping.checkout.application.port.outbound.NotificationSubmissionRepositoryPort
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:notification_submission_jpa_repository_adapter_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class NotificationSubmissionJpaRepositoryAdapterTest {
    @Autowired
    private lateinit var adapter: NotificationSubmissionRepositoryPort

    @Test
    fun `reserve retorna o vencedor persistido para replay da mesma notification key`() {
        val submission = pending()

        val created = adapter.reserve(submission)
        val replay = adapter.reserve(submission.copy(subject = "assunto que nao substitui o journal"))

        assertNotNull(created.id)
        assertEquals(created.id, replay.id)
        assertEquals(submission.subject, replay.subject)
    }

    @Test
    fun `claim e conclusao exigem o token atual da lease`() {
        val now = Instant.parse("2026-08-20T12:00:00Z")
        val id = requireNotNull(adapter.reserve(pending()).id)

        assertNotNull(adapter.claim(id, "lease-a", now.plusSeconds(30), now))
        assertNull(adapter.markAccepted(id, "lease-b", "ntf_remote", now))
        assertEquals(NotificationSubmissionStatus.ACCEPTED, adapter.markAccepted(id, "lease-a", "ntf_remote", now)?.status)
    }

    @Test
    fun `claim aceita falha e lease expirada mas rejeita lease ativa`() {
        val now = Instant.parse("2026-08-20T12:00:00Z")
        val id = requireNotNull(adapter.reserve(pending()).id)

        assertNotNull(adapter.claim(id, "lease-a", now.plusSeconds(30), now))
        assertNull(adapter.claim(id, "lease-b", now.plusSeconds(60), now.plusSeconds(1)))
        assertNotNull(adapter.claim(id, "lease-b", now.plusSeconds(90), now.plusSeconds(31)))
        assertNotNull(adapter.markFailed(id, "lease-b", "timeout", now.plusSeconds(32)))
        assertNotNull(adapter.claim(id, "lease-c", now.plusSeconds(120), now.plusSeconds(33)))
        assertEquals(3, adapter.findById(id)?.attemptCount)
    }

    @Test
    fun `discard so transiciona submissao pendente ou falha`() {
        val now = Instant.parse("2026-08-20T12:00:00Z")
        val pendingId = requireNotNull(adapter.reserve(pending()).id)
        val inFlightId = requireNotNull(adapter.reserve(pending()).id)

        assertEquals(NotificationSubmissionStatus.DISCARDED, adapter.discard(pendingId, "cancelada", now)?.status)
        assertNotNull(adapter.claim(inFlightId, "lease-a", now.plusSeconds(30), now))
        assertNull(adapter.discard(inFlightId, "cancelada", now))
    }

    @Test
    fun `findPage filtra status e usa ordenacao estavel por criacao e id`() {
        val first = adapter.reserve(pending())
        val second = adapter.reserve(pending())
        val now = Instant.parse("2026-08-20T12:00:00Z")
        adapter.discard(requireNotNull(second.id), "cancelada", now)

        val page = adapter.findPage(NotificationSubmissionStatus.PENDING, page = 0, size = 1)

        assertEquals(listOf(first.id), page.content.map { it.id })
        assertEquals(0, page.page)
        assertEquals(1, page.size)
        assertEquals(1, page.count)
        assertEquals(false, page.hasNext)
    }

    private fun pending(): NotificationSubmission {
        val sequence = UUID.randomUUID().toString()
        return NotificationSubmission(
            orderId = 10,
            customerId = 20,
            attemptReference = "attempt-$sequence",
            recipientEmail = "customer@example.com",
            notificationKey = "notification-$sequence",
            referenceId = "order-10",
            subject = "Pedido confirmado",
            body = "Seu pedido foi confirmado.",
        )
    }
}
