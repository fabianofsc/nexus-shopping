package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.ApplyOrderPaymentResultData
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData

interface OrderPaymentResultGateway {
    fun apply(data: ApplyOrderPaymentResultData): CheckoutOrderData
}
