package com.nexus.shopping.notification

import com.nexus.shopping.notification.application.command.SendNotificationCommand
import com.nexus.shopping.notification.application.port.outbound.EmailSendResult
import com.nexus.shopping.notification.application.port.outbound.EmailSenderPort
import com.nexus.shopping.notification.application.usecase.SendNotificationUseCase
import com.nexus.shopping.notification.domain.NotificationStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.transaction.support.TransactionSynchronizationManager
import kotlin.test.Test
import kotlin.test.assertEquals

private class TransactionCapturingEmailSender : EmailSenderPort {
    var transactionActiveDuringSend: Boolean? = null

    override fun send(
        to: String,
        subject: String,
        body: String,
    ): EmailSendResult {
        transactionActiveDuringSend = TransactionSynchronizationManager.isActualTransactionActive()
        return EmailSendResult(success = true)
    }
}

@TestConfiguration
private class TransactionBoundaryTestConfiguration {
    @Bean
    @Primary
    fun transactionCapturingEmailSender() = TransactionCapturingEmailSender()
}

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:notification_transaction_boundary_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@Import(TransactionBoundaryTestConfiguration::class)
class SendNotificationTransactionBoundaryTest {
    @Autowired
    private lateinit var useCase: SendNotificationUseCase

    @Autowired
    private lateinit var emailSender: TransactionCapturingEmailSender

    @Test
    fun `sends email outside repository transactions`() {
        val notification =
            useCase.send(
                SendNotificationCommand(
                    customerId = 1L,
                    notificationKey = "transaction-boundary-test",
                    recipientEmail = "cliente@example.com",
                    type = "ORDER_CONFIRMED",
                    referenceId = 123L,
                    templateParams = mapOf("orderId" to "123", "amount" to "99.90"),
                ),
            )

        assertEquals(NotificationStatus.SENT, notification.status)
        assertEquals(false, emailSender.transactionActiveDuringSend)
    }
}
