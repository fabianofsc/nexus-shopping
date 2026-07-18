package com.nexus.shopping.cart.application.usecase

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * A Customer never sees a 404 for their cart: the ACTIVE cart is created on first access
 * (getOrCreate) so callers always have somewhere to add items to.
 */
@Service
class GetActiveCartByCustomerIdUseCase(
    private val cartRepository: CartRepositoryPort,
) {
    fun execute(customerId: Long): Cart {
        logger.infoWithContext("cart.get_active.started", "cart.customer_id" to customerId)

        if (customerId <= 0) throwValidationFailed("customerId must be greater than 0.")

        val cart = cartRepository.getOrCreateActiveByCustomerId(customerId)

        logger.infoWithContext("cart.get_active.completed", "cart.id" to cart.id, "cart.customer_id" to customerId)
        return cart
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("cart.get_active.validation_failed", "validation.error" to message)
        throw CartValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(GetActiveCartByCustomerIdUseCase::class.java)
    }
}
