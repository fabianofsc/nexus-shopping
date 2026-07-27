package com.nexus.shopping.payment.adapter.outbound.provider

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingRequest
import com.nexus.shopping.payment.application.usecase.PaymentProviderDispatchKey
import com.nexus.shopping.payment.domain.PaymentAmount
import com.nexus.shopping.payment.domain.PaymentCurrency
import com.nexus.shopping.payment.domain.PaymentProvider
import com.nexus.shopping.payment.domain.PaymentStatus
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertContains
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:logging_payment_provider_gateway_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class LoggingPaymentProviderGatewayTest {
    @Autowired
    private lateinit var gateway: LoggingPaymentProviderGateway

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun clearJournal() {
        jdbcTemplate.update("DELETE FROM payment_provider_dispatches")
    }

    @Test
    fun `provider dispatch journal persists one decision for each dispatch key`() {
        DriverManager.getConnection("jdbc:h2:mem:payment_provider_dispatch_migration;DB_CLOSE_DELAY=-1", "sa", "").use { connection ->
            Flyway
                .configure()
                .dataSource("jdbc:h2:mem:payment_provider_dispatch_migration;DB_CLOSE_DELAY=-1", "sa", "")
                .locations("classpath:db/migration")
                .placeholders(mapOf("productSeedCount" to "10"))
                .load()
                .migrate()

            assertEquals(1, providerDispatchTableCount(connection))
        }
    }

    @Test
    fun `approved token produces an approved provider result`() {
        val result = gateway.process(request(paymentToken = "approved"))

        assertEquals(PaymentStatus.APPROVED, result.status)
    }

    @Test
    fun `a token other than approved produces a rejected provider result`() {
        val result = gateway.process(request(paymentToken = "declined"))

        assertEquals(PaymentStatus.REJECTED, result.status)
    }

    @Test
    fun `a repeated dispatch key returns the first persisted result and writes one journal row`() {
        val first = gateway.process(request(paymentToken = "declined"))
        val replay = gateway.process(request(paymentToken = "approved"))

        assertEquals(PaymentStatus.REJECTED, first.status)
        assertEquals(first, replay)
        assertEquals(1, journalCount())
    }

    @Test
    fun `a repeated dispatch key emits one dispatch log`() {
        val logger = LoggerFactory.getLogger(LoggingPaymentProviderGateway::class.java) as Logger
        val events = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(events)

        try {
            gateway.process(request(paymentToken = "declined"))
            gateway.process(request(paymentToken = "approved"))
        } finally {
            logger.detachAppender(events)
            events.stop()
        }

        assertEquals(1, events.list.size)
    }

    @Test
    fun `distinct references with the same HTTP key create independent journal entries`() {
        val httpIdempotencyKey = "http-key-42"
        val firstDispatchKey = dispatchKey(referenceId = "checkout-1", httpIdempotencyKey = httpIdempotencyKey)
        val secondDispatchKey = dispatchKey(referenceId = "checkout-2", httpIdempotencyKey = httpIdempotencyKey)
        val first = gateway.process(request(referenceId = "checkout-1", providerDispatchKey = firstDispatchKey))
        val second = gateway.process(request(referenceId = "checkout-2", providerDispatchKey = secondDispatchKey))

        assertEquals(PaymentStatus.REJECTED, first.status)
        assertEquals(PaymentStatus.REJECTED, second.status)
        assertFalse(firstDispatchKey == secondDispatchKey)
        assertEquals(2, journalCount())
    }

    @Test
    fun `dispatch log includes reference amount currency and key without the payment token`() {
        val token = "secret-payment-token"
        val logger = LoggerFactory.getLogger(LoggingPaymentProviderGateway::class.java) as Logger
        val events = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(events)

        try {
            gateway.process(request(paymentToken = token))
        } finally {
            logger.detachAppender(events)
            events.stop()
        }

        val message = events.list.single().formattedMessage
        assertContains(message, "referenceId=checkout-42")
        assertContains(message, "amount=19.90")
        assertContains(message, "currency=BRL")
        assertContains(message, "providerDispatchKey=dispatch-42")
        assertFalse(message.contains(token))
    }

    private fun providerDispatchTableCount(connection: Connection): Int =
        connection.createStatement().use { statement ->
            statement
                .executeQuery(
                    """
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_NAME = 'PAYMENT_PROVIDER_DISPATCHES'
                    """.trimIndent(),
                ).use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
        }

    private fun request(
        paymentToken: String = "declined",
        referenceId: String = "checkout-42",
        providerDispatchKey: String = "dispatch-42",
    ) =
        ProviderProcessingRequest(
            referenceId = referenceId,
            amount = PaymentAmount.of("19.90".toBigDecimal()),
            currency = PaymentCurrency.of("BRL"),
            paymentToken = paymentToken,
            providerDispatchKey = providerDispatchKey,
        )

    private fun journalCount(): Int =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM payment_provider_dispatches",
            Int::class.java,
        )!!

    private fun dispatchKey(
        referenceId: String,
        httpIdempotencyKey: String,
    ): String =
        PaymentProviderDispatchKey.current(
            provider = PaymentProvider.LOGGING_PROVIDER,
            referenceId = referenceId,
            idempotencyKey = httpIdempotencyKey,
        )
}
