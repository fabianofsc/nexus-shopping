package com.nexus.shopping.cart.application.usecase

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ClearCartUseCase(
    private val cartRepository: CartRepositoryPort,
) {
    fun execute(customerId: Long): Cart {
        logger.infoWithContext("cart.clear.started", "cart.customer_id" to customerId)

        if (customerId <= 0) throwValidationFailed("customerId must be greater than 0.")

        val existingCart = cartRepository.getOrCreateActiveByCustomerId(customerId)

        // The mutation is applied inside updateCart(), against a freshly re-read and locked cart,
        // not against `existingCart` above - that snapshot may already be stale by the time this
        // runs, e.g. a concurrent add/remove on the same cart may have committed in between.
        val updatedCart =
            cartRepository.updateCart(requireNotNull(existingCart.id)) { cart ->
                cart.withItemsCleared()
            }
        logger.infoWithContext("cart.clear.completed", "cart.id" to updatedCart.id)
        return updatedCart
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("cart.clear.validation_failed", "validation.error" to message)
        throw CartValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ClearCartUseCase::class.java)
    }
}
