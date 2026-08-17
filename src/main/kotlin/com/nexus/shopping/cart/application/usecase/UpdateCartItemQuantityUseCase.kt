package com.nexus.shopping.cart.application.usecase

import com.nexus.shopping.cart.application.command.UpdateCartItemQuantityCommand
import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartStatus
import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Adjusts the absolute quantity of an item already in the ACTIVE cart. Quantity 0 removes the
 * item (same effect as DELETE); a positive quantity sets the item's quantity. A product that is
 * not in the cart yet must be added via POST /cart/items first - the Cart context keeps receiving
 * denormalized product data from the caller and does not query the Catalog.
 */
@Service
class UpdateCartItemQuantityUseCase(
    private val cartRepository: CartRepositoryPort,
) {
    fun execute(command: UpdateCartItemQuantityCommand): Cart {
        logger.infoWithContext(
            "cart.update_item_quantity.started",
            "cart.customer_id" to command.customerId,
            "cart.product_id" to command.productId,
        )

        if (command.customerId <= 0) throwValidationFailed("customerId must be greater than 0.")
        if (command.productId <= 0) throwValidationFailed("productId must be greater than 0.")
        if (command.quantity < 0) throwValidationFailed("quantity must not be negative.")

        val existingCart = cartRepository.getOrCreateCartForMutationByCustomerId(command.customerId)

        // The mutation is applied inside updateCart(), against a freshly re-read and locked cart,
        // not against `existingCart` above - that snapshot may already be stale by the time this
        // runs, e.g. a concurrent add/remove on the same cart may have committed in between.
        val updatedCart =
            cartRepository.updateCart(requireNotNull(existingCart.id)) { cart ->
                if (cart.status != CartStatus.ACTIVE) throwValidationFailed("cart must be ACTIVE to update items.")
                if (command.quantity > 0 && cart.items.none { it.productSummary.productId == command.productId }) {
                    throwValidationFailed("productId ${command.productId} is not in the cart; add it via POST /cart/items first.")
                }
                cart.withItemQuantity(command.productId, command.quantity)
            }
        logger.infoWithContext(
            "cart.update_item_quantity.completed",
            "cart.id" to updatedCart.id,
            "cart.item_count" to updatedCart.items.size,
        )
        return updatedCart
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("cart.update_item_quantity.validation_failed", "validation.error" to message)
        throw CartValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(UpdateCartItemQuantityUseCase::class.java)
    }
}
