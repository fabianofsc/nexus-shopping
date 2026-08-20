package com.nexus.shopping.shipping.adapter.outbound.carrier

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.nexus.shopping.shipping.application.command.ProcessShippingCommand
import com.nexus.shopping.shipping.application.command.ShippingAddressSnapshot
import com.nexus.shopping.shipping.application.command.ShippingCustomerSnapshot
import com.nexus.shopping.shipping.application.command.ShippingItemSnapshot
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CarrierAdapterTest {
    @Test
    fun recordsFreightCalculationAndShipmentDispatchWithOrderReferenceOnly() {
        val logger = LoggerFactory.getLogger(CarrierAdapter::class.java) as Logger
        val events = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(events)

        try {
            val adapter = CarrierAdapter()
            val command = command()

            adapter.calculateFreight(command)
            adapter.dispatch(command)

            assertEquals(
                listOf(
                    "shipping.freight.calculated order_reference=checkout:1",
                    "shipping.shipment.dispatched order_reference=checkout:1",
                ),
                events.list.map { it.formattedMessage },
            )
            assertFalse(events.list.joinToString().contains("Rua A"))
        } finally {
            logger.detachAppender(events)
            events.stop()
        }
    }

    private fun command() =
        ProcessShippingCommand(
            orderId = 1L,
            orderReference = "checkout:1",
            customer = ShippingCustomerSnapshot("Ana Silva"),
            shippingAddress =
                ShippingAddressSnapshot(
                    street = "Rua A",
                    number = "10",
                    complement = null,
                    neighborhood = "Centro",
                    city = "Sao Paulo",
                    state = "SP",
                    zipCode = "01000-000",
                    country = "BR",
                ),
            items = listOf(ShippingItemSnapshot(1L, "Produto A", 2)),
            totalAmount = BigDecimal("39.80"),
        )
}
