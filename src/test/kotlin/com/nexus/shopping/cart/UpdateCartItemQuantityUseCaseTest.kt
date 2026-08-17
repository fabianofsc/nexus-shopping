package com.nexus.shopping.cart

import com.nexus.shopping.cart.application.command.UpdateCartItemQuantityCommand
import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.application.usecase.UpdateCartItemQuantityUseCase
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartItem
import com.nexus.shopping.cart.domain.CartStatus
import com.nexus.shopping.cart.domain.Currency
import com.nexus.shopping.cart.domain.ProductSummary
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeUpdateQuantityCartRepository(
    var activeCart: Cart? = null,
) : CartRepositoryPort {
    val savedCarts = mutableListOf<Cart>()
    private var nextId = 1L

    override fun findActiveByCustomerId(customerId: Long): Cart? = activeCart

    override fun reserveActiveCart(customerId: Long): Cart? = findActiveByCustomerId(customerId)

    override fun confirmCheckout(reservationId: Long) = error("Not used by update quantity")

    override fun getOrCreateActiveByCustomerId(customerId: Long): Cart =
        activeCart ?: save(
            Cart(
                id = null,
                customerId = customerId,
                status = CartStatus.ACTIVE,
                items = emptyList(),
                createdAt = null,
                updatedAt = null,
            ),
        )

    override fun updateCart(
        cartId: Long,
        mutate: (Cart) -> Cart,
    ): Cart {
        val current = savedCarts.lastOrNull { it.id == cartId } ?: activeCart?.takeIf { it.id == cartId }
        return save(mutate(requireNotNull(current) { "No cart with id $cartId found." }))
    }

    private fun save(cart: Cart): Cart {
        val persisted =
            cart.copy(
                id = cart.id ?: nextId++,
                createdAt = cart.createdAt ?: Instant.parse("2026-07-17T12:00:00Z"),
                updatedAt = Instant.parse("2026-07-17T12:00:00Z"),
            )
        savedCarts += persisted
        activeCart = persisted.takeIf { it.status == CartStatus.ACTIVE }
        return persisted
    }
}

class UpdateCartItemQuantityUseCaseTest {
    private fun cartWithItem() =
        Cart(
            id = 1L,
            customerId = 1L,
            status = CartStatus.ACTIVE,
            items =
                listOf(
                    CartItem(
                        productSummary = ProductSummary(10L, "Product 10", BigDecimal("19.90"), Currency.BRL),
                        quantity = 2,
                    ),
                ),
            createdAt = Instant.parse("2026-07-17T12:00:00Z"),
            updatedAt = Instant.parse("2026-07-17T12:00:00Z"),
        )

    private fun command(
        customerId: Long = 1L,
        productId: Long = 10L,
        quantity: Int = 5,
    ) = UpdateCartItemQuantityCommand(customerId, productId, quantity)

    @Test
    fun `sets the absolute quantity of an existing item`() {
        val useCase = UpdateCartItemQuantityUseCase(FakeUpdateQuantityCartRepository(activeCart = cartWithItem()))

        val cart = useCase.execute(command(quantity = 5))

        assertEquals(5, cart.items.single().quantity)
    }

    @Test
    fun `removes the item when quantity is zero`() {
        val useCase = UpdateCartItemQuantityUseCase(FakeUpdateQuantityCartRepository(activeCart = cartWithItem()))

        val cart = useCase.execute(command(quantity = 0))

        assertEquals(emptyList(), cart.items)
    }

    @Test
    fun `throws CartValidationException when the product is not in the cart`() {
        val useCase = UpdateCartItemQuantityUseCase(FakeUpdateQuantityCartRepository(activeCart = cartWithItem()))

        assertFailsWith<CartValidationException> {
            useCase.execute(command(productId = 99L, quantity = 1))
        }
    }

    @Test
    fun `throws CartValidationException when quantity is negative`() {
        val useCase = UpdateCartItemQuantityUseCase(FakeUpdateQuantityCartRepository(activeCart = cartWithItem()))

        assertFailsWith<CartValidationException> {
            useCase.execute(command(quantity = -1))
        }
    }

    @Test
    fun `throws CartValidationException when customerId or productId is not positive`() {
        val useCase = UpdateCartItemQuantityUseCase(FakeUpdateQuantityCartRepository(activeCart = cartWithItem()))

        assertFailsWith<CartValidationException> { useCase.execute(command(customerId = 0L)) }
        assertFailsWith<CartValidationException> { useCase.execute(command(productId = 0L)) }
    }
}
