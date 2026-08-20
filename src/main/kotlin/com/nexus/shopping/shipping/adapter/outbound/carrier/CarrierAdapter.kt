package com.nexus.shopping.shipping.adapter.outbound.carrier

import com.nexus.shopping.shipping.application.command.ProcessShippingCommand
import com.nexus.shopping.shipping.application.port.outbound.CarrierPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CarrierAdapter : CarrierPort {
    override fun calculateFreight(command: ProcessShippingCommand) {
        logger.info("shipping.freight.calculated order_reference={}", command.orderReference)
        // TODO: substituir o log pela integracao com a transportadora.
    }

    override fun dispatch(command: ProcessShippingCommand) {
        logger.info("shipping.shipment.dispatched order_reference={}", command.orderReference)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(CarrierAdapter::class.java)
    }
}
