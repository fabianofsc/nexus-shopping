package com.nexus.shopping.payment.adapter.outbound.jpa

import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptRepositoryPort
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptReservation
import com.nexus.shopping.payment.domain.PaymentAmount
import com.nexus.shopping.payment.domain.PaymentAttempt
import com.nexus.shopping.payment.domain.PaymentCurrency
import com.nexus.shopping.payment.domain.PaymentProvider
import com.nexus.shopping.payment.domain.PaymentStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:payment_jpa_repository_adapter_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class PaymentJpaRepositoryAdapterTest {
    @Autowired
    private lateinit var attempts: PaymentAttemptRepositoryPort

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `reserve persists fingerprint and returns existing winner for an idempotent replay`() {
        val requested = requested(referenceId = "order-reserve", idempotencyKey = "payment-reserve")

        val created = attempts.reserve(requested)
        val replay = attempts.reserve(requested.copyWithNewAttemptReference())

        assertIs<PaymentAttemptReservation.Created>(created)
        assertIs<PaymentAttemptReservation.Existing>(replay)
        assertNotNull(created.attempt.id)
        assertEquals(created.attempt.id, replay.attempt.id)
        assertEquals(created.attempt.attemptReference, replay.attempt.attemptReference)
        assertEquals(requested.authorizationFingerprint, replay.attempt.authorizationFingerprint)
        assertEquals(1, count("order-reserve", "payment-reserve"))
    }

    @Test
    fun `complete only accepts the current lease token and returns the persisted terminal attempt`() {
        val requested = requested(referenceId = "order-fencing", idempotencyKey = "payment-fencing")
        val created = assertIs<PaymentAttemptReservation.Created>(attempts.reserve(requested)).attempt
        val replacementToken = "replacement-${UUID.randomUUID()}"
        jdbcTemplate.update(
            "UPDATE payment_attempts SET processing_lease_token = ? WHERE attempt_reference = ?",
            replacementToken,
            created.attemptReference,
        )

        val staleCompletion =
            attempts.complete(
                created.attemptReference,
                requested.processingLeaseToken!!,
                PaymentStatus.APPROVED,
                "provider-stale",
                Instant.parse("2026-07-26T12:01:00Z"),
            )
        val currentCompletion =
            attempts.complete(
                created.attemptReference,
                replacementToken,
                PaymentStatus.APPROVED,
                "provider-current",
                Instant.parse("2026-07-26T12:02:00Z"),
            )

        assertNull(staleCompletion)
        assertNotNull(currentCompletion)
        assertNotNull(currentCompletion.id)
        assertEquals(PaymentStatus.APPROVED, currentCompletion.status)
        assertEquals("provider-current", currentCompletion.providerTransactionId)
        assertNull(currentCompletion.processingLeaseToken)
    }

    @Test
    fun `concurrent reserve exposes one created owner and one existing winner without a uniqueness exception`() {
        val barrier = CyclicBarrier(2)
        val first = requested(referenceId = "order-concurrent", idempotencyKey = "payment-concurrent")
        val second = first.copyWithNewAttemptReference()
        val executor = Executors.newFixedThreadPool(2)

        try {
            val results =
                listOf(first, second).map { attempt ->
                    executor.submit(
                        Callable {
                            barrier.await(5, TimeUnit.SECONDS)
                            attempts.reserve(attempt)
                        },
                    )
                }.map { future -> future.get(10, TimeUnit.SECONDS) }

            assertEquals(1, results.count { it is PaymentAttemptReservation.Created })
            assertEquals(1, results.count { it is PaymentAttemptReservation.Existing })
            assertEquals(1, count("order-concurrent", "payment-concurrent"))
        } finally {
            executor.shutdownNow()
        }
    }

    private fun requested(
        referenceId: String,
        idempotencyKey: String,
    ) =
        PaymentAttempt.requested(
            attemptReference = "pay-${UUID.randomUUID()}",
            referenceId = referenceId,
            amount = PaymentAmount.of("19.90".toBigDecimal()),
            currency = PaymentCurrency.of("BRL"),
            provider = PaymentProvider.LOGGING_PROVIDER,
            idempotencyKey = idempotencyKey,
            authorizationFingerprint = "fingerprint-${UUID.randomUUID()}",
            processingLeaseToken = "lease-${UUID.randomUUID()}",
            processingLeaseUntil = Instant.parse("2026-07-26T12:05:00Z"),
            createdAt = Instant.parse("2026-07-26T12:00:00Z"),
        )

    private fun PaymentAttempt.copyWithNewAttemptReference() =
        PaymentAttempt.requested(
            attemptReference = "pay-${UUID.randomUUID()}",
            referenceId = referenceId,
            amount = amount,
            currency = currency,
            provider = provider,
            idempotencyKey = idempotencyKey,
            authorizationFingerprint = authorizationFingerprint,
            processingLeaseToken = processingLeaseToken!!,
            processingLeaseUntil = processingLeaseUntil!!,
            createdAt = createdAt!!,
        )

    private fun count(
        referenceId: String,
        idempotencyKey: String,
    ): Int =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM payment_attempts WHERE reference_id = ? AND idempotency_key = ?",
            Int::class.java,
            referenceId,
            idempotencyKey,
        )!!
}
