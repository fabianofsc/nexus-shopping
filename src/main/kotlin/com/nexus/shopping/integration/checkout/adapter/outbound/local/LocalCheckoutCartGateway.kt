package com.nexus.shopping.integration.checkout.adapter.outbound.local

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.inbound.CartCheckoutInputPort
import com.nexus.shopping.integration.checkout.application.exception.CheckoutValidationException
import com.nexus.shopping.integration.checkout.application.model.CheckoutCartData
import com.nexus.shopping.integration.checkout.application.model.CheckoutItemData
import com.nexus.shopping.integration.checkout.application.port.outbound.CheckoutCartGateway
import org.springframework.stereotype.Component

@Component
class LocalCheckoutCartGateway(
    private val carts: CartCheckoutInputPort,
) : CheckoutCartGateway {
    override fun reserveActiveCart(customerId: Long): CheckoutCartData =
        try {
            val reservation = carts.reserveActiveCart(customerId)
            CheckoutCartData(
                reservationId = reservation.id,
                customerId = reservation.customerId,
                items =
                    reservation.items.map { item ->
                        CheckoutItemData(
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
