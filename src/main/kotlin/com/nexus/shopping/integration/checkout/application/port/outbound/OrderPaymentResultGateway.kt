package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.ApplyOrderPaymentResultCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderSnapshot

interface OrderPaymentResultGateway {
    fun apply(command: ApplyOrderPaymentResultCommand): CheckoutOrderSnapshot
}
