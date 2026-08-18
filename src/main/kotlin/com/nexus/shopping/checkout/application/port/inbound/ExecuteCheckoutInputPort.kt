package com.nexus.shopping.checkout.application.port.inbound

import com.nexus.shopping.checkout.application.model.CheckoutCommand
import com.nexus.shopping.checkout.application.model.CheckoutOrderSnapshot

interface ExecuteCheckoutInputPort {
    fun execute(command: CheckoutCommand): CheckoutOrderSnapshot
}
