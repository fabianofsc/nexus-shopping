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
}
