package com.nexus.shopping.cart.domain

import java.time.Instant

data class Cart(
    val id: Long?,
    val customerId: Long,
    val status: CartStatus,
    val items: List<CartItem>,
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    /**
     * Adding a productId that is already in the cart sums the quantity into the existing item and
     * replaces its ProductSummary with the one just received (most recent name/price/currency wins).
     * The cart in this context only ever displays denormalized product data; it is not the historical
     * truth of the purchase, so overwriting stale display data on every add is the expected behavior.
     */
    fun withItemAdded(
        productSummary: ProductSummary,
        quantity: Int,
    ): Cart {
        val existing = items.find { it.productSummary.productId == productSummary.productId }
        val updatedItems =
            if (existing != null) {
                items.map {
                    if (it.productSummary.productId == productSummary.productId) {
                        CartItem(productSummary = productSummary, quantity = it.quantity + quantity)
                    } else {
                        it
                    }
                }
            } else {
                items + CartItem(productSummary = productSummary, quantity = quantity)
            }
        return copy(items = updatedItems)
    }

    fun withItemRemoved(productId: Long): Cart = copy(items = items.filterNot { it.productSummary.productId == productId })

    fun withItemsCleared(): Cart = copy(items = emptyList())
}
