package com.nexus.shopping.payment

import com.nexus.shopping.payment.application.exception.PaymentProviderGatewayException
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptRepositoryPort
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptReservation
import com.nexus.shopping.payment.application.port.outbound.PaymentProviderGateway
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingRequest
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingResult
import com.nexus.shopping.payment.application.port.outbound.ProviderStatusResult
import com.nexus.shopping.payment.application.usecase.ReconcilePendingPaymentAttemptsUseCase
import com.nexus.shopping.payment.domain.PaymentAmount
import com.nexus.shopping.payment.domain.PaymentAttempt
import com.nexus.shopping.payment.domain.PaymentCurrency
import com.nexus.shopping.payment.domain.PaymentProvider
import com.nexus.shopping.payment.domain.PaymentStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReconcilePendingPaymentAttemptsUseCaseTest {
    @Test
    fun `attempts still processing at the provider do not appear in the result`() {
        val repository = PaymentAttemptRepositoryFake()
        repository.seedDispatched(providerAttemptReference = "nexus-1")
        val gateway = FakeProviderGateway(mapOf("nexus-1" to ProviderStatusResult(PaymentStatus.REQUESTED, null)))

        val results = ReconcilePendingPaymentAttemptsUseCase(repository, gateway).reconcile()

        assertEquals(emptyList(), results)
        assertEquals(PaymentStatus.REQUESTED, repository.attempts.single().status)
    }

    @Test
    fun `terminal attempts complete locally and appear once in the result`() {
        val repository = PaymentAttemptRepositoryFake()
        val seeded = repository.seedDispatched(providerAttemptReference = "nexus-1")
        val gateway = FakeProviderGateway(mapOf("nexus-1" to ProviderStatusResult(PaymentStatus.APPROVED, "provider-tx-1")))

        val results = ReconcilePendingPaymentAttemptsUseCase(repository, gateway).reconcile()

        assertEquals(1, results.size)
        assertEquals(seeded.attemptReference, results.single().attemptReference)
        assertEquals(seeded.referenceId, results.single().referenceId)
        assertEquals(PaymentStatus.APPROVED, results.single().status)
        assertEquals("provider-tx-1", results.single().providerTransactionId)
        assertEquals(PaymentStatus.APPROVED, repository.attempts.single().status)
    }

    @Test
    fun `a repeated reconcile call does not reprocess an already completed attempt`() {
        val repository = PaymentAttemptRepositoryFake()
        repository.seedDispatched(providerAttemptReference = "nexus-1")
        val gateway = FakeProviderGateway(mapOf("nexus-1" to ProviderStatusResult(PaymentStatus.APPROVED, "provider-tx-1")))
        val useCase = ReconcilePendingPaymentAttemptsUseCase(repository, gateway)

        val first = useCase.reconcile()
        val second = useCase.reconcile()

        assertEquals(1, first.size)
        assertTrue(second.isEmpty())
    }

    @Test
    fun `a failure reconciling one attempt does not prevent other attempts from being reconciled`() {
        val repository = PaymentAttemptRepositoryFake()
        val failing = repository.seedDispatched(providerAttemptReference = "nexus-1")
        val succeeding = repository.seedDispatched(providerAttemptReference = "nexus-2")
        val gateway =
            FakeProviderGateway(
                statuses = mapOf("nexus-2" to ProviderStatusResult(PaymentStatus.APPROVED, "provider-tx-2")),
                failing = setOf("nexus-1"),
            )

        val results = ReconcilePendingPaymentAttemptsUseCase(repository, gateway).reconcile()

        assertEquals(1, results.size)
        assertEquals(succeeding.attemptReference, results.single().attemptReference)
        assertEquals(PaymentStatus.APPROVED, results.single().status)
        assertEquals(PaymentStatus.REQUESTED, repository.attempts.first { it.attemptReference == failing.attemptReference }.status)
        assertEquals(PaymentStatus.APPROVED, repository.attempts.first { it.attemptReference == succeeding.attemptReference }.status)
    }

    private class FakeProviderGateway(
        private val statuses: Map<String, ProviderStatusResult>,
        private val failing: Set<String> = emptySet(),
    ) : PaymentProviderGateway {
        override val provider = PaymentProvider.PAYMENT_SERVICE

        override fun process(request: ProviderProcessingRequest): ProviderProcessingResult =
            throw UnsupportedOperationException("Not used by this fake.")

        override fun checkStatus(providerAttemptReference: String): ProviderStatusResult {
            if (providerAttemptReference in failing) {
                throw PaymentProviderGatewayException("nexus-payment-service returned 502 (unknown).")
            }
            return requireNotNull(statuses[providerAttemptReference]) { "No stubbed status for $providerAttemptReference" }
        }
    }

    private class PaymentAttemptRepositoryFake : PaymentAttemptRepositoryPort {
        val attempts = mutableListOf<PaymentAttempt>()

        fun seedDispatched(providerAttemptReference: String): PaymentAttempt {
            val index = attempts.size + 1
            val attempt =
                PaymentAttempt
                    .requested(
                        attemptReference = "pay_$index",
                        referenceId = "checkout:$index",
                        amount = PaymentAmount.of("19.90".toBigDecimal()),
                        currency = PaymentCurrency.of("BRL"),
                        provider = PaymentProvider.PAYMENT_SERVICE,
                        idempotencyKey = "idem-$index",
                        authorizationFingerprint = "fingerprint-$index",
                        processingLeaseToken = "lease-$index",
                        processingLeaseUntil = Instant.now().plusSeconds(30),
                        createdAt = Instant.now(),
                    ).recordProviderDispatch(providerAttemptReference)
            attempts += attempt
            return attempt
        }

        override fun reserve(attempt: PaymentAttempt): PaymentAttemptReservation =
            throw UnsupportedOperationException("Not used by this fake.")

        override fun findByReferenceIdAndIdempotencyKey(
            referenceId: String,
            idempotencyKey: String,
        ): PaymentAttempt? = throw UnsupportedOperationException("Not used by this fake.")

        override fun complete(
            attemptReference: String,
            processingLeaseToken: String,
            status: PaymentStatus,
            providerTransactionId: String?,
            completedAt: Instant,
        ): PaymentAttempt? {
            val current = attempts.firstOrNull { it.attemptReference == attemptReference } ?: return null
            if (current.processingLeaseToken != processingLeaseToken) return null
            val completed = current.complete(status, providerTransactionId, completedAt)
            attempts[attempts.indexOf(current)] = completed
            return completed
        }

        override fun recordProviderDispatch(
            attemptReference: String,
            processingLeaseToken: String,
            providerAttemptReference: String,
        ): PaymentAttempt? = throw UnsupportedOperationException("Not used by this fake.")

        override fun findPendingByProvider(
            provider: PaymentProvider,
            limit: Int,
        ): List<PaymentAttempt> = attempts.filter { it.status == PaymentStatus.REQUESTED && it.provider == provider }.take(limit)
    }
}
