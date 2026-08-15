package com.nexus.shopping.integration.checkout.adapter.inbound.scheduler

import com.nexus.shopping.integration.checkout.application.PaymentReconciliationUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PaymentReconciliationScheduler(
    private val reconciliation: PaymentReconciliationUseCase,
) {
    @Scheduled(fixedDelayString = "\${nexus.payment-service.polling-interval:2000}")
    fun reconcile() {
        reconciliation.reconcile()
    }
}
