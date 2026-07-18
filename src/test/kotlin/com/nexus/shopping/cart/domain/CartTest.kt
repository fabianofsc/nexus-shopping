package com.nexus.shopping.cart.domain

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class CartTest {
    private fun emptyCart() =
        Cart(
            id = null,
            customerId = 1L,
            status = CartStatus.ACTIVE,
            items = emptyList(),
            createdAt = null,
            updatedAt = null,
        )

    private fun summary(
        productId: Long = 10L,
        name: String = "Product 10",
        price: String = "19.90",
    ) = ProductSummary(
        productId = productId,
        name = name,
        unitPriceAmount = BigDecimal(price),
        currency = Currency.BRL,
    )

    @Test
    fun `withItemAdded appends a new item when productId is not present`() {
        val cart = emptyCart().withItemAdded(summary(), 2)

        assertEquals(1, cart.items.size)
        assertEquals(10L, cart.items[0].productSummary.productId)
        assertEquals(2, cart.items[0].quantity)
    }

    @Test
    fun `withItemAdded sums quantity and replaces the product summary when productId already exists`() {
        val cart =
            emptyCart()
                .withItemAdded(summary(price = "19.90"), 2)
                .withItemAdded(summary(name = "Product 10 updated", price = "24.90"), 3)

        assertEquals(1, cart.items.size)
        assertEquals(5, cart.items[0].quantity)
        assertEquals("Product 10 updated", cart.items[0].productSummary.name)
        assertEquals(BigDecimal("24.90"), cart.items[0].productSummary.unitPriceAmount)
    }

    @Test
    fun `withItemRemoved removes only the matching productId`() {
        val cart =
            emptyCart()
                .withItemAdded(summary(productId = 10L), 1)
                .withItemAdded(summary(productId = 20L), 1)
                .withItemRemoved(10L)

        assertEquals(1, cart.items.size)
        assertEquals(20L, cart.items[0].productSummary.productId)
    }

    @Test
    fun `withItemRemoved is a no-op when productId is not present`() {
        val cart = emptyCart().withItemAdded(summary(productId = 10L), 1).withItemRemoved(999L)

        assertEquals(1, cart.items.size)
    }

    @Test
    fun `withItemsCleared removes all items and keeps the cart ACTIVE`() {
        val cart = emptyCart().withItemAdded(summary(), 1).withItemsCleared()

        assertEquals(emptyList(), cart.items)
        assertEquals(CartStatus.ACTIVE, cart.status)
    }
}
