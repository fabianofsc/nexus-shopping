package com.nexus.shopping.payment.adapter.outbound.provider

import com.nexus.shopping.payment.application.port.outbound.PaymentProviderGateway
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingRequest
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingResult
import com.nexus.shopping.payment.domain.PaymentStatus
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.UUID

@Component
class LoggingPaymentProviderGateway(
    private val repository: SpringDataPaymentProviderDispatchRepository,
    transactionManager: PlatformTransactionManager,
) : PaymentProviderGateway {
    private val transactions =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    override fun process(request: ProviderProcessingRequest): ProviderProcessingResult {
        val dispatch =
            try {
                recordInNewTransaction(request)
            } catch (exception: DataIntegrityViolationException) {
                findRecordedDispatchInNewTransaction(request.providerDispatchKey) ?: throw exception
            }

        if (dispatch.created) {
            logger.info(
                "Simulated payment provider dispatched referenceId={} amount={} currency={} providerDispatchKey={}",
                request.referenceId,
                request.amount.value,
                request.currency.code,
                request.providerDispatchKey,
            )
        }
        return dispatch.result
    }

    private fun recordInNewTransaction(request: ProviderProcessingRequest): RecordedDispatch =
        requireNotNull(
            transactions.execute {
                val existing = repository.findByProviderDispatchKey(request.providerDispatchKey).orElse(null)
                if (existing != null) {
                    RecordedDispatch(existing.toResult(), created = false)
                } else {
                    val status = if (request.paymentToken == APPROVED_TOKEN) PaymentStatus.APPROVED else PaymentStatus.REJECTED
                    val result = ProviderProcessingResult(status, "log_${UUID.randomUUID()}")
                    val saved =
                        repository.saveAndFlush(
                            PaymentProviderDispatchEntity(
                                providerDispatchKey = request.providerDispatchKey,
                                referenceId = request.referenceId,
                                amount = request.amount.value,
                                currency = request.currency.code,
                                status = result.status,
                                providerTransactionId = result.providerTransactionId,
                                createdAt = Instant.now(),
                            ),
                        )
                    RecordedDispatch(saved.toResult(), created = true)
                }
            },
        )

    private fun findRecordedDispatchInNewTransaction(providerDispatchKey: String): RecordedDispatch? =
        transactions.execute {
            repository.findByProviderDispatchKey(providerDispatchKey).orElse(null)?.let {
                RecordedDispatch(it.toResult(), created = false)
            }
        }

    private data class RecordedDispatch(
        val result: ProviderProcessingResult,
        val created: Boolean,
    )

    private companion object {
        private const val APPROVED_TOKEN = "approved"
        private val logger = LoggerFactory.getLogger(LoggingPaymentProviderGateway::class.java)
    }
}
