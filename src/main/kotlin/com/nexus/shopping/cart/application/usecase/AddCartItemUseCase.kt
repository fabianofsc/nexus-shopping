package com.nexus.shopping.cart.application.usecase

import com.nexus.shopping.cart.application.command.AddCartItemCommand
import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartStatus
import com.nexus.shopping.cart.domain.Currency
import com.nexus.shopping.cart.domain.ProductSummary
import com.nexus.shopping.platform.application.logging.infoWithContext
import com.nexus.shopping.platform.application.logging.warnWithContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class AddCartItemUseCase(
    private val cartRepository: CartRepositoryPort,
) {
    fun execute(command: AddCartItemCommand): Cart {
        logger.infoWithContext(
            "cart.add_item.started",
            "cart.customer_id" to command.customerId,
            "cart.product_id" to command.productId,
        )

        if (command.customerId <= 0) throwValidationFailed("customerId must be greater than 0.")
        if (command.productId <= 0) throwValidationFailed("productId must be greater than 0.")
        if (command.productName.isBlank()) throwValidationFailed("productName must not be blank.")
        if (command.quantity <= 0) throwValidationFailed("quantity must be greater than 0.")
        if (command.unitPriceAmount < BigDecimal.ZERO) throwValidationFailed("unitPriceAmount must be >= 0.")
        val currency = requireValidCurrency(command.currency)

        val productSummary =
            ProductSummary(
                productId = command.productId,
                name = command.productName,
                unitPriceAmount = command.unitPriceAmount,
                currency = currency,
            )

        val existingCart = cartRepository.getOrCreateCartForMutationByCustomerId(command.customerId)

        // The mutation is applied inside updateCart(), against a freshly re-read and locked cart,
        // not against `existingCart` above - that snapshot may already be stale by the time this
        // runs, e.g. a concurrent add/remove on the same cart may have committed in between.
        val updatedCart =
            cartRepository.updateCart(requireNotNull(existingCart.id)) { cart ->
                if (cart.status != CartStatus.ACTIVE) throwValidationFailed("cart must be ACTIVE to add items.")
                cart.withItemAdded(productSummary, command.quantity)
            }
        logger.infoWithContext(
            "cart.add_item.completed",
            "cart.id" to updatedCart.id,
            "cart.item_count" to updatedCart.items.size,
        )
        return updatedCart
    }

    private fun requireValidCurrency(value: String): Currency {
        val names = Currency.entries.map { it.name }
        if (value !in names) throwValidationFailed("currency must be one of: ${names.joinToString(", ")}.")
        return Currency.valueOf(value)
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("cart.add_item.validation_failed", "validation.error" to message)
        throw CartValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(AddCartItemUseCase::class.java)
    }
}
