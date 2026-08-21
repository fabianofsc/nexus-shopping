package com.nexus.shopping.checkout

import com.nexus.shopping.checkout.application.model.EnsureOrderConfirmationCommand
import com.nexus.shopping.checkout.application.model.NotificationSubmission
import com.nexus.shopping.checkout.application.port.outbound.NotificationSubmissionRepositoryPort
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:notification_submission_checkout_integration_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class NotificationSubmissionCheckoutIntegrationTest {
    @Autowired
    private lateinit var repository: NotificationSubmissionRepositoryPort

    @Test
    fun `replay persists one notification submission for the approved order`() {
        val command =
            EnsureOrderConfirmationCommand(
                orderId = 1L,
                customerId = 10L,
                recipientEmail = "ana@example.com",
                amount = BigDecimal("39.80"),
                attemptReference = "attempt-${UUID.randomUUID()}",
            )

        val first = repository.reserve(NotificationSubmission.forOrderConfirmation(command))
        val replay = repository.reserve(NotificationSubmission.forOrderConfirmation(command))

        assertEquals(first.id, replay.id)
        assertEquals(1, repository.findPage(null, page = 0, size = 10).count)
    }
}
