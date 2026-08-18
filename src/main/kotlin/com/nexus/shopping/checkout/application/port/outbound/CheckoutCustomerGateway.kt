package com.nexus.shopping.checkout.application.port.outbound

import com.nexus.shopping.checkout.application.model.CheckoutCustomerResolution

interface CheckoutCustomerGateway {
    fun resolve(customerId: Long): CheckoutCustomerResolution
}
