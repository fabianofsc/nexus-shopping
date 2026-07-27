package com.nexus.shopping.cart.application.usecase

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.inbound.CartCheckoutInputPort
import com.nexus.shopping.cart.application.port.inbound.CartCheckoutReservation
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import org.springframework.stereotype.Service

@Service
class CartCheckoutUseCase(
    private val cartRepository: CartRepositoryPort,
) : CartCheckoutInputPort {
    override fun reserveActiveCart(customerId: Long): CartCheckoutReservation {
        if (customerId <= 0) throw CartValidationException("customerId must be greater than zero.")
        val cart =
            cartRepository.reserveActiveCart(customerId)
                ?: throw CartValidationException("customerId $customerId does not have an active cart.")
        return CartCheckoutReservation(cart)
    }

    override fun confirmCheckout(reservationId: Long) {
        if (reservationId <= 0) throw CartValidationException("reservationId must be greater than zero.")
        cartRepository.confirmCheckout(reservationId)
    }
}
