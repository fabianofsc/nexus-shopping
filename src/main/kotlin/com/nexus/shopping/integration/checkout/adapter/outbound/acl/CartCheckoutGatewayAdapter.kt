package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.inbound.CartCheckoutInputPort
import com.nexus.shopping.integration.checkout.application.exception.CheckoutValidationException
import com.nexus.shopping.integration.checkout.application.model.CheckoutCartSnapshot
import com.nexus.shopping.integration.checkout.application.model.CheckoutItemSnapshot
import com.nexus.shopping.integration.checkout.application.port.outbound.CheckoutCartGateway
import org.springframework.stereotype.Component

@Component
class CartCheckoutGatewayAdapter(
    private val carts: CartCheckoutInputPort,
) : CheckoutCartGateway {
    override fun reserveActiveCart(customerId: Long): CheckoutCartSnapshot =
        try {
            val reservation = carts.reserveActiveCart(customerId)
            CheckoutCartSnapshot(
                reservationId = reservation.id,
                customerId = reservation.customerId,
                items =
                    reservation.items.map { item ->
                        CheckoutItemSnapshot(
                            productId = item.productSummary.productId,
                            productName = item.productSummary.name,
                            unitPriceAmount = item.productSummary.unitPriceAmount,
                            currency = item.productSummary.currency.name,
                            quantity = item.quantity,
                        )
                    },
            )
        } catch (exception: CartValidationException) {
            throw CheckoutValidationException(requireNotNull(exception.message))
        }

    override fun confirmCheckout(reservationId: Long) = carts.confirmCheckout(reservationId)
}
