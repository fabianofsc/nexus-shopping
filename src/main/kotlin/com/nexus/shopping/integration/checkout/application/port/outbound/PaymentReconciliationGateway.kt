package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.PaymentReconciliationOutcome

interface PaymentReconciliationGateway {
    fun reconcile(): List<PaymentReconciliationOutcome>
}
