package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.PaymentReconciliationOutcome

interface PaymentReconciliationGateway {
    fun reconcile(): List<PaymentReconciliationOutcome>
}
