package com.nexus.shopping.integration.checkout

import com.nexus.shopping.integration.checkout.application.CheckoutWorkflowUseCase
import com.nexus.shopping.integration.checkout.application.model.CheckoutCartData
import com.nexus.shopping.integration.checkout.application.model.CheckoutCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutCustomerData
import com.nexus.shopping.integration.checkout.application.model.CheckoutItemData
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData
import com.nexus.shopping.integration.checkout.application.model.CheckoutShippingAddressData
import com.nexus.shopping.integration.checkout.application.model.CreateOrderData
import com.nexus.shopping.integration.checkout.application.port.outbound.CheckoutCartGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderCreationGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.TransactionPort
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckoutWorkflowUseCaseTest {
    @Test
    fun `coordinates reserve create and confirm inside one transaction`() {
        val events = mutableListOf<String>()
        val carts = RecordingCartGateway(events)
        val orders = RecordingOrderGateway(events)
        val transactions =
            object : TransactionPort {
                override fun <T> inTransaction(block: () -> T): T {
                    events += "transaction:start"
                    return block().also { events += "transaction:end" }
                }
            }

        val result = CheckoutWorkflowUseCase(carts, orders, transactions).execute(command())

        assertEquals(
            listOf("transaction:start", "replay", "reserve", "replay", "create", "confirm", "transaction:end"),
            events,
        )
        assertEquals(false, result.replayed)
        assertEquals(100L, orders.createdData?.cartId)
        assertEquals(listOf(item()), orders.createdData?.items)
    }

    @Test
    fun `returns replay before touching Cart`() {
        val events = mutableListOf<String>()
        val replay = order(replayed = true)
        val carts = RecordingCartGateway(events)
        val orders = RecordingOrderGateway(events, replay = replay)

        val result = CheckoutWorkflowUseCase(carts, orders, ImmediateTransaction).execute(command())

        assertEquals(replay, result)
        assertEquals(listOf("replay"), events)
    }

    private fun command() =
        CheckoutCommand(
            customerId = 10L,
            customerSnapshot = CheckoutCustomerData(10L, "Ana Silva", "12345678900", "CPF", "ana@example.com", null),
            shippingAddressSnapshot =
                CheckoutShippingAddressData("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
            idempotencyKey = "checkout-1",
        )

    private fun item() = CheckoutItemData(1L, "Produto A", BigDecimal("19.90"), "BRL", 2)

    private fun order(replayed: Boolean) =
        CheckoutOrderData(
            id = 1L,
            customerId = 10L,
            cartId = 100L,
            customerSnapshot = command().customerSnapshot,
            shippingAddressSnapshot = command().shippingAddressSnapshot,
            items = listOf(item()),
            totalAmount = BigDecimal("39.80"),
            status = "WAITING_PAYMENT",
            createdAt = Instant.parse("2026-07-26T12:00:00Z"),
            cancelledAt = null,
            replayed = replayed,
        )

    private inner class RecordingCartGateway(
        private val events: MutableList<String>,
    ) : CheckoutCartGateway {
        override fun reserveActiveCart(customerId: Long): CheckoutCartData {
            events += "reserve"
            return CheckoutCartData(100L, customerId, listOf(item()))
        }

        override fun confirmCheckout(reservationId: Long) {
            events += "confirm"
        }
    }

    private inner class RecordingOrderGateway(
        private val events: MutableList<String>,
        private val replay: CheckoutOrderData? = null,
    ) : OrderCreationGateway {
        var createdData: CreateOrderData? = null

        override fun findReplay(command: CheckoutCommand): CheckoutOrderData? {
            events += "replay"
            return replay
        }

        override fun create(data: CreateOrderData): CheckoutOrderData {
            events += "create"
            createdData = data
            return order(replayed = false)
        }
    }

    private object ImmediateTransaction : TransactionPort {
        override fun <T> inTransaction(block: () -> T): T = block()
    }
}
