package com.nexus.shopping.cart.application.usecase

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Removing an item is idempotent: if the customer has no ACTIVE cart yet, or the productId is not
 * in it, this still returns a (possibly newly created, empty) ACTIVE cart rather than a 404 -
 * consistent with the getOrCreate behavior used across the Cart context.
 */
@Service
class RemoveCartItemUseCase(
    private val cartRepository: CartRepositoryPort,
) {
    fun execute(
        customerId: Long,
        productId: Long,
    ): Cart {
        logger.infoWithContext(
            "cart.remove_item.started",
            "cart.customer_id" to customerId,
            "cart.product_id" to productId,
        )

        if (customerId <= 0) throwValidationFailed("customerId must be greater than 0.")
        if (productId <= 0) throwValidationFailed("productId must be greater than 0.")

        val existingCart = cartRepository.getOrCreateActiveByCustomerId(customerId)

        // The mutation is applied inside updateCart(), against a freshly re-read and locked cart,
        // not against `existingCart` above - that snapshot may already be stale by the time this
        // runs, e.g. a concurrent add/remove on the same cart may have committed in between.
        val updatedCart =
            cartRepository.updateCart(requireNotNull(existingCart.id)) { cart ->
                cart.withItemRemoved(productId)
            }
        logger.infoWithContext(
            "cart.remove_item.completed",
            "cart.id" to updatedCart.id,
            "cart.item_count" to updatedCart.items.size,
        )
        return updatedCart
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("cart.remove_item.validation_failed", "validation.error" to message)
        throw CartValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(RemoveCartItemUseCase::class.java)
    }
}
