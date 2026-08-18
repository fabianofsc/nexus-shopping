package com.nexus.shopping.shipping.application.port.inbound

import com.nexus.shopping.shipping.application.command.ProcessShippingCommand

interface ProcessShippingInputPort {
    fun process(command: ProcessShippingCommand)
}
