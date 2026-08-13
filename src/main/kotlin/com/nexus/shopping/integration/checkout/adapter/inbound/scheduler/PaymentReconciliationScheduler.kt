package com.nexus.shopping.integration.checkout.adapter.inbound.scheduler

import com.nexus.shopping.integration.checkout.application.PaymentReconciliationUseCase
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "nexus.payment-service", name = ["enabled"], havingValue = "true")
class PaymentReconciliationScheduler(
    private val reconciliation: PaymentReconciliationUseCase,
) {
    @Scheduled(fixedDelayString = "\${nexus.payment-service.polling-interval:2000}")
    fun reconcile() {
        reconciliation.reconcile()
    }
}
