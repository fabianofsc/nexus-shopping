package com.nexus.shopping.shipping

import com.nexus.shopping.shipping.application.command.ProcessShippingCommand
import com.nexus.shopping.shipping.application.command.ShippingAddressSnapshot
import com.nexus.shopping.shipping.application.command.ShippingCustomerSnapshot
import com.nexus.shopping.shipping.application.command.ShippingItemSnapshot
import com.nexus.shopping.shipping.application.port.outbound.CarrierPort
import com.nexus.shopping.shipping.application.usecase.ProcessShippingUseCase
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessShippingUseCaseTest {
    @Test
    fun calculatesFreightBeforeDispatchingShipment() {
        val calls = mutableListOf<String>()
        val useCase =
            ProcessShippingUseCase(
                object : CarrierPort {
                    override fun calculateFreight(command: ProcessShippingCommand) {
                        calls += "calculate"
                    }

                    override fun dispatch(command: ProcessShippingCommand) {
                        calls += "dispatch"
                    }
                },
            )

        useCase.process(shippingCommand())

        assertEquals(listOf("calculate", "dispatch"), calls)
    }

    private fun shippingCommand() =
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
