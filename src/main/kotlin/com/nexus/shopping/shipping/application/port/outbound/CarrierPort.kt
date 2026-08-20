package com.nexus.shopping.shipping.application.port.outbound

import com.nexus.shopping.shipping.application.command.ProcessShippingCommand

interface CarrierPort {
    fun calculateFreight(command: ProcessShippingCommand)

    fun dispatch(command: ProcessShippingCommand)
}
