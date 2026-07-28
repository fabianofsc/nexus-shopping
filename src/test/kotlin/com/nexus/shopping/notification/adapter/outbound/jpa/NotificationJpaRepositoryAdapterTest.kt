package com.nexus.shopping.notification.adapter.outbound.jpa

import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationChannel
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.notification.domain.NotificationType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:notification_jpa_repository_adapter_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@Transactional
class NotificationJpaRepositoryAdapterTest {
    @Autowired
    private lateinit var repository: NotificationJpaRepositoryAdapter

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `save persists type, channel and status as their enum name, not an ordinal`() {
        val notification =
            Notification(
                id = null,
                customerId = 1L,
                notificationKey = "notification-adapter-enum",
                recipientEmail = "cliente@example.com",
                type = NotificationType.ORDER_PAYMENT_FAILED,
                channel = NotificationChannel.EMAIL,
                status = NotificationStatus.FAILED,
                subject = "Falha no pagamento",
                body = "Nao foi possivel processar o pagamento.",
                referenceId = 456L,
                createdAt = null,
                sentAt = null,
            )

        val saved = repository.save(notification)

        val row =
            jdbcTemplate.queryForMap(
                "SELECT type, channel, status FROM notifications WHERE id = ?",
                saved.id,
            )

        assertEquals("ORDER_PAYMENT_FAILED", row["TYPE"])
        assertEquals("EMAIL", row["CHANNEL"])
        assertEquals("FAILED", row["STATUS"])
    }

    @Test
    fun `findByCustomerId reads back the enum name correctly through Hibernate`() {
        val notification =
            Notification(
                id = null,
                customerId = 1L,
                notificationKey = "notification-adapter-read",
                recipientEmail = "cliente@example.com",
                type = NotificationType.ORDER_CANCELLED,
                channel = NotificationChannel.EMAIL,
                status = NotificationStatus.SENT,
                subject = "Pedido cancelado",
                body = "Seu pedido foi cancelado.",
                referenceId = null,
                createdAt = null,
                sentAt = Instant.now(),
            )
        repository.save(notification)

        val page = repository.findByCustomerId(customerId = 1L, page = 0, size = 50)

        val found = page.content.single { it.type == NotificationType.ORDER_CANCELLED }
        assertEquals(NotificationChannel.EMAIL, found.channel)
        assertEquals(NotificationStatus.SENT, found.status)
    }

    @Test
    fun `reserve deduplicates by notification key`() {
        val first = repository.reserve(pendingNotification("order-confirmed:123:attempt-1"))
        val replay = repository.reserve(pendingNotification("order-confirmed:123:attempt-1"))

        assertEquals(first.id, replay.id)
        assertEquals(
            1,
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE notification_key = ?",
                Int::class.java,
                "order-confirmed:123:attempt-1",
            ),
        )
    }

    @Test
    fun `expired SENDING lease can be reclaimed and stale owner cannot complete`() {
        repository.reserve(pendingNotification("order-confirmed:123:attempt-2"))
        val firstNow = Instant.parse("2026-07-28T12:00:00Z")
        val first =
            requireNotNull(
                repository.claim(
                    notificationKey = "order-confirmed:123:attempt-2",
                    sendingLeaseToken = "owner-1",
                    sendingLeaseUntil = firstNow.plusSeconds(30),
                    now = firstNow,
                ),
            )
        assertEquals(NotificationStatus.SENDING, first.status)
        assertEquals(
            null,
            repository.claim(
                notificationKey = "order-confirmed:123:attempt-2",
                sendingLeaseToken = "owner-too-early",
                sendingLeaseUntil = firstNow.plusSeconds(40),
                now = firstNow.plusSeconds(20),
            ),
        )

        val reclaimed =
            requireNotNull(
                repository.claim(
                    notificationKey = "order-confirmed:123:attempt-2",
                    sendingLeaseToken = "owner-2",
                    sendingLeaseUntil = firstNow.plusSeconds(70),
                    now = firstNow.plusSeconds(31),
                ),
            )
        assertEquals("owner-2", reclaimed.sendingLeaseToken)

        assertEquals(
            null,
            repository.complete(
                notificationKey = "order-confirmed:123:attempt-2",
                sendingLeaseToken = "owner-1",
                status = NotificationStatus.FAILED,
                sentAt = null,
            ),
        )
        val sent =
            requireNotNull(
                repository.complete(
                    notificationKey = "order-confirmed:123:attempt-2",
                    sendingLeaseToken = "owner-2",
                    status = NotificationStatus.SENT,
                    sentAt = firstNow.plusSeconds(32),
                ),
            )
        assertEquals(NotificationStatus.SENT, sent.status)
        assertEquals(null, sent.sendingLeaseToken)
        assertEquals(null, sent.sendingLeaseUntil)
    }

    @Test
    fun `FAILED notification can be claimed for another delivery attempt`() {
        repository.reserve(pendingNotification("order-confirmed:123:attempt-3"))
        val now = Instant.parse("2026-07-28T12:00:00Z")
        repository.claim(
            notificationKey = "order-confirmed:123:attempt-3",
            sendingLeaseToken = "owner-1",
            sendingLeaseUntil = now.plusSeconds(30),
            now = now,
        )
        repository.complete(
            notificationKey = "order-confirmed:123:attempt-3",
            sendingLeaseToken = "owner-1",
            status = NotificationStatus.FAILED,
            sentAt = null,
        )

        val retry =
            requireNotNull(
                repository.claim(
                    notificationKey = "order-confirmed:123:attempt-3",
                    sendingLeaseToken = "owner-2",
                    sendingLeaseUntil = now.plusSeconds(60),
                    now = now.plusSeconds(1),
                ),
            )

        assertEquals(NotificationStatus.SENDING, retry.status)
        assertEquals("owner-2", retry.sendingLeaseToken)
    }

    private fun pendingNotification(notificationKey: String) =
        Notification(
            id = null,
            customerId = 1L,
            notificationKey = notificationKey,
            recipientEmail = "cliente@example.com",
            type = NotificationType.ORDER_CONFIRMED,
            channel = NotificationChannel.EMAIL,
            status = NotificationStatus.PENDING,
            subject = "Pedido 123 confirmado",
            body = "Seu pedido 123 no valor de 99.90 foi confirmado.",
            referenceId = 123L,
            createdAt = null,
            sentAt = null,
        )
}
