package com.nexus.shopping.checkout.adapter.inbound.scheduler

import com.nexus.shopping.checkout.application.port.inbound.ReconcilePaymentsInputPort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PaymentReconciliationScheduler(
    private val reconciliation: ReconcilePaymentsInputPort,
) {
    @Scheduled(fixedDelayString = "\${nexus.payment-service.polling-interval:2000}")
    fun reconcile() {
        reconciliation.reconcile()
    }
}
