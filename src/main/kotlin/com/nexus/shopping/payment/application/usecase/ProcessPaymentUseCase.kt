package com.nexus.shopping.payment.application.usecase

import com.nexus.shopping.payment.application.command.ProcessPaymentCommand
import com.nexus.shopping.payment.application.command.ValidatePaymentInputCommand
import com.nexus.shopping.payment.application.exception.PaymentIdempotencyConflictException
import com.nexus.shopping.payment.application.exception.PaymentValidationException
import com.nexus.shopping.payment.application.port.inbound.PaymentProcessingResult
import com.nexus.shopping.payment.application.port.inbound.ProcessPaymentInputPort
import com.nexus.shopping.payment.application.port.inbound.ValidatePaymentInputPort
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptRepositoryPort
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptReservation
import com.nexus.shopping.payment.application.port.outbound.PaymentAuthorizationFingerprintSecretPort
import com.nexus.shopping.payment.application.port.outbound.PaymentProviderGateway
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingRequest
import com.nexus.shopping.payment.domain.PaymentAmount
import com.nexus.shopping.payment.domain.PaymentAttempt
import com.nexus.shopping.payment.domain.PaymentCurrency
import com.nexus.shopping.payment.domain.PaymentDomainValidationException
import com.nexus.shopping.payment.domain.PaymentProvider
import com.nexus.shopping.payment.domain.PaymentStatus
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class ProcessPaymentUseCase(
    private val paymentAttemptRepository: PaymentAttemptRepositoryPort,
    private val paymentProviderGateway: PaymentProviderGateway,
    private val fingerprintSecret: PaymentAuthorizationFingerprintSecretPort,
) : ProcessPaymentInputPort,
    ValidatePaymentInputPort {
    override fun validate(command: ValidatePaymentInputCommand) {
        try {
            PaymentAmount.of(command.amount)
            PaymentCurrency.of(command.currency)
        } catch (exception: PaymentDomainValidationException) {
            throw PaymentValidationException(exception.message ?: "Invalid payment input.")
        }
    }

    override fun process(command: ProcessPaymentCommand): PaymentProcessingResult {
        validate(ValidatePaymentInputCommand(command.amount, command.currency))
        requireText(command.referenceId, "referenceId")
        requireText(command.paymentToken, "paymentToken")
        requireText(command.idempotencyKey, "idempotencyKey")
        maximumLength(command.referenceId, "referenceId", 255)
        maximumLength(command.idempotencyKey, "idempotencyKey", 255)

        val amount = PaymentAmount.of(command.amount)
        val currency = PaymentCurrency.of(command.currency)
        val authorizationFingerprint =
            PaymentAuthorizationFingerprint.current(
                referenceId = command.referenceId,
                amount = amount,
                currency = currency,
                paymentToken = command.paymentToken,
                idempotencyKey = command.idempotencyKey,
                secret = fingerprintSecret.secret(),
            )
        val attempt =
            PaymentAttempt.requested(
                attemptReference = "pay_${UUID.randomUUID().toString().replace("-", "")}",
                referenceId = command.referenceId,
                amount = amount,
                currency = currency,
                provider = PaymentProvider.LOGGING_PROVIDER,
                idempotencyKey = command.idempotencyKey,
                authorizationFingerprint = authorizationFingerprint,
                processingLeaseToken = UUID.randomUUID().toString(),
                processingLeaseUntil = Instant.now().plusSeconds(30),
                createdAt = Instant.now(),
            )

        return when (val reservation = paymentAttemptRepository.reserve(attempt)) {
            is PaymentAttemptReservation.Existing ->
                replay(
                    reservation.attempt,
                    authorizationFingerprint,
                )
            is PaymentAttemptReservation.Created -> processReservedAttempt(reservation.attempt, command.paymentToken)
        }
    }

    private fun replay(
        attempt: PaymentAttempt,
        authorizationFingerprint: String,
    ): PaymentProcessingResult {
        if (!MessageDigest.isEqual(
                attempt.authorizationFingerprint.toByteArray(UTF_8),
                authorizationFingerprint.toByteArray(UTF_8),
            )
        ) {
            throw PaymentIdempotencyConflictException(
                "Idempotency key ${attempt.idempotencyKey} was already used with a different payment authorization.",
            )
        }
        return awaitTerminalResult(attempt).toProcessingResult(replayed = true)
    }

    private fun awaitTerminalResult(attempt: PaymentAttempt): PaymentAttempt {
        if (attempt.status != PaymentStatus.REQUESTED) return attempt

        val deadline = System.nanoTime() + REQUESTED_WAIT_TIMEOUT_MILLIS * NANOS_PER_MILLISECOND
        var current = attempt
        while (current.status == PaymentStatus.REQUESTED && System.nanoTime() < deadline) {
            try {
                Thread.sleep(REQUESTED_POLL_INTERVAL_MILLIS)
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                return current
            }
            current =
                paymentAttemptRepository.findByReferenceIdAndIdempotencyKey(
                    referenceId = attempt.referenceId,
                    idempotencyKey = attempt.idempotencyKey,
                ) ?: current
        }
        return current
    }

    private fun processReservedAttempt(
        attempt: PaymentAttempt,
        paymentToken: String,
    ): PaymentProcessingResult {
        val providerResult =
            paymentProviderGateway.process(
                ProviderProcessingRequest(
                    referenceId = attempt.referenceId,
                    amount = attempt.amount,
                    currency = attempt.currency,
                    paymentToken = paymentToken,
                    providerDispatchKey =
                        PaymentProviderDispatchKey.current(
                            provider = attempt.provider,
                            referenceId = attempt.referenceId,
                            idempotencyKey = attempt.idempotencyKey,
                        ),
                ),
            )
        val completed =
            paymentAttemptRepository.complete(
                attemptReference = attempt.attemptReference,
                processingLeaseToken = requireNotNull(attempt.processingLeaseToken),
                status = providerResult.status,
                providerTransactionId = providerResult.providerTransactionId,
                completedAt = Instant.now(),
            ) ?: attempt
        return completed.toProcessingResult(replayed = false)
    }

    private fun requireText(
        value: String,
        field: String,
    ) {
        if (value.isBlank()) throw PaymentValidationException("$field must not be blank.")
    }

    private fun maximumLength(
        value: String,
        field: String,
        maximum: Int,
    ) {
        if (value.length > maximum) throw PaymentValidationException("$field must not exceed $maximum characters.")
    }
}

private fun PaymentAttempt.toProcessingResult(replayed: Boolean) =
    PaymentProcessingResult(
        attemptReference = attemptReference,
        referenceId = referenceId,
        status = status,
        providerTransactionId = providerTransactionId,
        replayed = replayed,
    )

internal object PaymentAuthorizationFingerprint {
    fun current(
        referenceId: String,
        amount: PaymentAmount,
        currency: PaymentCurrency,
        paymentToken: String,
        idempotencyKey: String,
        secret: ByteArray,
    ): String =
        hmac(
            buildString {
                appendField(referenceId)
                appendField(amount.value.toPlainString())
                appendField(currency.code)
                appendField(paymentToken)
                appendField(idempotencyKey)
            },
            secret,
        )

    private fun hmac(
        canonicalPayload: String,
        secret: ByteArray,
    ): String =
        Mac
            .getInstance("HmacSHA256")
            .apply { init(SecretKeySpec(secret, "HmacSHA256")) }
            .doFinal(canonicalPayload.toByteArray(UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
}

internal object PaymentProviderDispatchKey {
    fun current(
        provider: PaymentProvider,
        referenceId: String,
        idempotencyKey: String,
    ): String =
        "v1_" +
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    buildString {
                        appendField(provider.name)
                        appendField(referenceId)
                        appendField(idempotencyKey)
                    }.toByteArray(UTF_8),
                ).joinToString("") { byte -> "%02x".format(byte) }
}

private const val REQUESTED_WAIT_TIMEOUT_MILLIS = 500L
private const val REQUESTED_POLL_INTERVAL_MILLIS = 10L
private const val NANOS_PER_MILLISECOND = 1_000_000L

private fun StringBuilder.appendField(value: String) {
    append(value.toByteArray(UTF_8).size).append(':').append(value)
}
