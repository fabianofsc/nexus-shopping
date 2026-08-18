package com.nexus.shopping.integration.checkout.application.port.outbound

import com.nexus.shopping.integration.checkout.application.model.CheckoutCustomerResolution

interface CheckoutCustomerGateway {
    fun resolve(customerId: Long): CheckoutCustomerResolution
}
