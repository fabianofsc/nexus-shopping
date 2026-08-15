package com.nexus.shopping.payment.application.usecase

import com.nexus.shopping.payment.application.port.inbound.PaymentReconciliationResult
import com.nexus.shopping.payment.application.port.inbound.ReconcilePendingPaymentAttemptsInputPort
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptRepositoryPort
import com.nexus.shopping.payment.application.port.outbound.PaymentProviderGateway
import com.nexus.shopping.payment.domain.PaymentAttempt
import com.nexus.shopping.payment.domain.PaymentProvider
import com.nexus.shopping.payment.domain.PaymentStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ReconcilePendingPaymentAttemptsUseCase(
    private val paymentAttemptRepository: PaymentAttemptRepositoryPort,
    private val paymentProviderGateway: PaymentProviderGateway,
) : ReconcilePendingPaymentAttemptsInputPort {
    override fun reconcile(): List<PaymentReconciliationResult> =
        paymentAttemptRepository
            .findPendingByProvider(PaymentProvider.PAYMENT_SERVICE)
            .mapNotNull { attempt ->
                try {
                    reconcileAttempt(attempt)
                } catch (exception: RuntimeException) {
                    // One attempt's failure (e.g. a stale/unknown provider reference, or a
                    // provider-side error) must not stop the rest of the batch from being
                    // reconciled - the oldest pending attempt failing forever would otherwise
                    // silently stall every other checkout.
                    logger.warn(
                        "Failed to reconcile payment attempt attemptReference={} providerAttemptReference={}",
                        attempt.attemptReference,
                        attempt.providerAttemptReference,
                        exception,
                    )
                    null
                }
            }

    private fun reconcileAttempt(attempt: PaymentAttempt): PaymentReconciliationResult? {
        val providerAttemptReference = attempt.providerAttemptReference ?: return null
        val statusResult = paymentProviderGateway.checkStatus(providerAttemptReference)
        if (statusResult.status == PaymentStatus.REQUESTED) return null

        val completed =
            paymentAttemptRepository.complete(
                attemptReference = attempt.attemptReference,
                processingLeaseToken = requireNotNull(attempt.processingLeaseToken),
                status = statusResult.status,
                providerTransactionId = statusResult.providerTransactionId,
                completedAt = Instant.now(),
            ) ?: return null

        return PaymentReconciliationResult(
            attemptReference = completed.attemptReference,
            referenceId = completed.referenceId,
            status = completed.status,
            providerTransactionId = completed.providerTransactionId,
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger(ReconcilePendingPaymentAttemptsUseCase::class.java)
    }
}
