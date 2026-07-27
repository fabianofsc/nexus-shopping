package com.nexus.shopping.payment

import com.nexus.shopping.payment.application.command.ProcessPaymentCommand
import com.nexus.shopping.payment.application.exception.PaymentIdempotencyConflictException
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptRepositoryPort
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptReservation
import com.nexus.shopping.payment.application.port.outbound.PaymentAuthorizationFingerprintSecretPort
import com.nexus.shopping.payment.application.port.outbound.PaymentProviderGateway
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingRequest
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingResult
import com.nexus.shopping.payment.application.usecase.ProcessPaymentUseCase
import com.nexus.shopping.payment.domain.PaymentAmount
import com.nexus.shopping.payment.domain.PaymentAttempt
import com.nexus.shopping.payment.domain.PaymentCurrency
import com.nexus.shopping.payment.domain.PaymentProvider
import com.nexus.shopping.payment.domain.PaymentStatus
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProcessPaymentUseCaseTest {
    @Test
    fun `processes an opaque token and exposes only the approved result`() {
        val repository = PaymentAttemptRepositoryFake()
        val result =
            ProcessPaymentUseCase(repository, ApprovedProvider(), FixedFingerprintSecret()).process(command(paymentToken = "opaque-token"))

        assertEquals(PaymentStatus.APPROVED, result.status)
        assertEquals("provider-tx-1", result.providerTransactionId)
        assertEquals(false, result.replayed)
        assertTrue(result.attemptReference.startsWith("pay_"))
        assertEquals(
            "a522033ce2401cf44d22150676765f6173fa7190032c7684c344c9f278b60ea5",
            repository.attempts.single().authorizationFingerprint,
        )
        assertTrue(repository.attempts.single().toString().contains("opaque-token").not())
    }

    @Test
    fun `derives a versioned size prefixed provider dispatch key`() {
        val provider = ApprovedProvider()

        ProcessPaymentUseCase(PaymentAttemptRepositoryFake(), provider, FixedFingerprintSecret()).process(command())

        assertEquals(
            "v1_edb959fdea610389a04d691e46b65164dbbac2fde27d29776972ff8e3e257869",
            provider.requests.single().providerDispatchKey,
        )
    }

    @Test
    fun `replays a terminal attempt with the same authorization without dispatching again`() {
        val repository = PaymentAttemptRepositoryFake()
        val provider = ApprovedProvider()
        val useCase = ProcessPaymentUseCase(repository, provider, FixedFingerprintSecret())

        val original = useCase.process(command())
        val replay = useCase.process(command())

        assertEquals(original.attemptReference, replay.attemptReference)
        assertEquals(PaymentStatus.APPROVED, replay.status)
        assertEquals(true, replay.replayed)
        assertEquals(1, provider.requests.size)
    }

    @Test
    fun `rejects reuse of a reference and idempotency key with a different token`() {
        val useCase = ProcessPaymentUseCase(PaymentAttemptRepositoryFake(), ApprovedProvider(), FixedFingerprintSecret())
        useCase.process(command())

        assertFailsWith<PaymentIdempotencyConflictException> {
            useCase.process(command(paymentToken = "another-opaque-token"))
        }
    }

    private fun command(paymentToken: String = "opaque-token") =
        ProcessPaymentCommand(
            referenceId = "checkout:42",
            amount = BigDecimal("19.90"),
            currency = "BRL",
            paymentToken = paymentToken,
            idempotencyKey = "checkout-key-1",
        )
}

private class FixedFingerprintSecret : PaymentAuthorizationFingerprintSecretPort {
    override fun secret(): ByteArray = "test-payment-secret".toByteArray()
}

private class ApprovedProvider : PaymentProviderGateway {
    val requests = mutableListOf<ProviderProcessingRequest>()

    override fun process(request: ProviderProcessingRequest): ProviderProcessingResult {
        requests += request
        return ProviderProcessingResult(PaymentStatus.APPROVED, "provider-tx-1")
    }
}

private class PaymentAttemptRepositoryFake : PaymentAttemptRepositoryPort {
    val attempts = mutableListOf<PaymentAttempt>()

    override fun reserve(attempt: PaymentAttempt): PaymentAttemptReservation {
        val existing =
            attempts.firstOrNull {
                it.referenceId == attempt.referenceId && it.idempotencyKey == attempt.idempotencyKey
            }
        if (existing != null) return PaymentAttemptReservation.Existing(existing)
        attempts += attempt
        return PaymentAttemptReservation.Created(attempt)
    }

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
}
