package com.nexus.shopping.shipping.application.usecase

import com.nexus.shopping.shipping.application.command.ProcessShippingCommand
import com.nexus.shopping.shipping.application.port.inbound.ProcessShippingInputPort
import com.nexus.shopping.shipping.application.port.outbound.CarrierPort
import org.springframework.stereotype.Service

@Service
class ProcessShippingUseCase(
    private val carrier: CarrierPort,
) : ProcessShippingInputPort {
    override fun process(command: ProcessShippingCommand) {
        carrier.calculateFreight(command)
        carrier.dispatch(command)
    }
}
