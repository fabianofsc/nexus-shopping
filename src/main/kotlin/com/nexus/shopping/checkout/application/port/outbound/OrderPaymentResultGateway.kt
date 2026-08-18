package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.ApplyOrderPaymentResultCommand
import com.nexus.shopping.checkout.application.model.CheckoutOrderSnapshot

interface OrderPaymentResultGateway {
    fun apply(command: ApplyOrderPaymentResultCommand): CheckoutOrderSnapshot
}
