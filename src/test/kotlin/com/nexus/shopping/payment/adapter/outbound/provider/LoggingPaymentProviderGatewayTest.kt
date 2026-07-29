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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

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

    @Autowired
    private lateinit var repository: SpringDataPaymentProviderDispatchRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

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
        assertNotNull(result.providerTransactionId)
        assertNotEquals("dispatch-42", result.providerTransactionId)
        assertEquals(result.providerTransactionId, journalProviderTransactionId())

        val replay = gateway.process(request(paymentToken = "declined"))

        assertEquals(result, replay)
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
    fun `concurrent dispatches with the same key persist one result and emit one log`() {
        val persistenceBarrier = CyclicBarrier(2)
        val integrityViolations = AtomicInteger()
        val concurrentGateway =
            LoggingPaymentProviderGateway(
                repositoryWithPersistenceBarrier(persistenceBarrier, integrityViolations),
                transactionManager,
            )
        val logger = LoggerFactory.getLogger(LoggingPaymentProviderGateway::class.java) as Logger
        val events = ListAppender<ILoggingEvent>().apply { start() }
        val executor = Executors.newFixedThreadPool(2)
        logger.addAppender(events)

        try {
            val results =
                listOf("approved", "declined")
                    .map { token ->
                        executor.submit(
                            Callable {
                                concurrentGateway.process(request(paymentToken = token))
                            },
                        )
                    }.map { future -> future.get(10, TimeUnit.SECONDS) }

            assertEquals(results.first(), results.last())
            assertNotNull(results.first().providerTransactionId)
            assertEquals(1, journalCount())
            assertEquals(1, events.list.size)
            assertEquals(1, integrityViolations.get())
        } finally {
            logger.detachAppender(events)
            events.stop()
            executor.shutdownNow()
        }
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
    ) = ProviderProcessingRequest(
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

    private fun journalProviderTransactionId(): String =
        requireNotNull(
            jdbcTemplate.queryForObject(
                "SELECT provider_transaction_id FROM payment_provider_dispatches WHERE provider_dispatch_key = ?",
                String::class.java,
                "dispatch-42",
            ),
        )

    @Suppress("UNCHECKED_CAST")
    private fun repositoryWithPersistenceBarrier(
        barrier: CyclicBarrier,
        integrityViolations: AtomicInteger,
    ): SpringDataPaymentProviderDispatchRepository =
        Proxy.newProxyInstance(
            SpringDataPaymentProviderDispatchRepository::class.java.classLoader,
            arrayOf(SpringDataPaymentProviderDispatchRepository::class.java),
        ) { _, method, arguments ->
            if (method.name == "saveAndFlush") barrier.await(5, TimeUnit.SECONDS)
            try {
                method.invoke(repository, *(arguments ?: emptyArray()))
            } catch (exception: InvocationTargetException) {
                if (exception.targetException is DataIntegrityViolationException) integrityViolations.incrementAndGet()
                throw exception.targetException
            }
        } as SpringDataPaymentProviderDispatchRepository

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
