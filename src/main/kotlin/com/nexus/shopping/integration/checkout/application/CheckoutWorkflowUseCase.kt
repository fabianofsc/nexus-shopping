package com.nexus.shopping.integration.checkout.application

import com.nexus.shopping.integration.checkout.application.exception.CheckoutValidationException
import com.nexus.shopping.integration.checkout.application.model.CheckoutCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData
import com.nexus.shopping.integration.checkout.application.model.CreateOrderData
import com.nexus.shopping.integration.checkout.application.port.outbound.CheckoutCartGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderCreationGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.TransactionPort
import org.springframework.stereotype.Service

@Service
class CheckoutWorkflowUseCase(
    private val carts: CheckoutCartGateway,
    private val orders: OrderCreationGateway,
    private val transaction: TransactionPort,
) {
    fun execute(command: CheckoutCommand): CheckoutOrderData =
        transaction.inTransaction {
            orders.findReplay(command)?.let { return@inTransaction it }

            val cart =
                try {
                    carts.reserveActiveCart(command.customerId)
                } catch (exception: CheckoutValidationException) {
                    orders.findReplay(command)?.let { return@inTransaction it }
                    throw exception
                }
            orders.findReplay(command)?.let { return@inTransaction it }
            if (cart.items.isEmpty()) throw CheckoutValidationException("cart items must not be empty.")

            val order =
                orders.create(
                    CreateOrderData(
                        customerId = command.customerId,
                        cartId = cart.reservationId,
                        customerSnapshot = command.customerSnapshot,
                        shippingAddressSnapshot = command.shippingAddressSnapshot,
                        items = cart.items,
                        idempotencyKey = command.idempotencyKey,
                    ),
                )
            if (!order.replayed && order.cartId == cart.reservationId) {
                carts.confirmCheckout(cart.reservationId)
            }
            order
        }
}
