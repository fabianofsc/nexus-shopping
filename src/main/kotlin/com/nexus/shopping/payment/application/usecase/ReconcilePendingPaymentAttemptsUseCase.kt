package com.nexus.shopping.payment.application.usecase

import com.nexus.shopping.payment.application.port.inbound.PaymentReconciliationResult
import com.nexus.shopping.payment.application.port.inbound.ReconcilePendingPaymentAttemptsInputPort
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptRepositoryPort
import com.nexus.shopping.payment.application.port.outbound.PaymentProviderGateway
import com.nexus.shopping.payment.domain.PaymentAttempt
import com.nexus.shopping.payment.domain.PaymentProvider
import com.nexus.shopping.payment.domain.PaymentStatus
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
            .mapNotNull(::reconcileAttempt)

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
}
