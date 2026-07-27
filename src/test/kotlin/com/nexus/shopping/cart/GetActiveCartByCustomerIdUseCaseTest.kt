package com.nexus.shopping.cart

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.application.usecase.GetActiveCartByCustomerIdUseCase
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeGetActiveCartRepository : CartRepositoryPort {
    val savedCarts = mutableListOf<Cart>()
    var activeCart: Cart? = null
    private var nextId = 1L

    override fun findActiveByCustomerId(customerId: Long): Cart? = activeCart

    override fun reserveActiveCart(customerId: Long): Cart? = findActiveByCustomerId(customerId)

    override fun confirmCheckout(reservationId: Long) = error("Not used by active cart lookup")

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
                createdAt = cart.createdAt ?: java.time.Instant.parse("2026-07-17T12:00:00Z"),
                updatedAt = java.time.Instant.parse("2026-07-17T12:00:00Z"),
            )
        savedCarts += persisted
        activeCart = persisted.takeIf { it.status == CartStatus.ACTIVE }
        return persisted
    }
}

class GetActiveCartByCustomerIdUseCaseTest {
    @Test
    fun `returns existing ACTIVE cart without creating a new one`() {
        val repository = FakeGetActiveCartRepository()
        repository.activeCart =
            Cart(
                id = 5L,
                customerId = 1L,
                status = CartStatus.ACTIVE,
                items = emptyList(),
                createdAt = java.time.Instant.parse("2026-07-17T12:00:00Z"),
                updatedAt = java.time.Instant.parse("2026-07-17T12:00:00Z"),
            )
        val useCase = GetActiveCartByCustomerIdUseCase(repository)

        val cart = useCase.execute(1L)

        assertEquals(5L, cart.id)
        assertEquals(0, repository.savedCarts.size)
    }

    @Test
    fun `creates and persists a new empty ACTIVE cart when none exists`() {
        val repository = FakeGetActiveCartRepository()
        val useCase = GetActiveCartByCustomerIdUseCase(repository)

        val cart = useCase.execute(1L)

        assertEquals(1L, cart.customerId)
        assertEquals(CartStatus.ACTIVE, cart.status)
        assertEquals(emptyList(), cart.items)
        assertEquals(1, repository.savedCarts.size)
    }

    @Test
    fun `throws CartValidationException when customerId is not positive`() {
        val useCase = GetActiveCartByCustomerIdUseCase(FakeGetActiveCartRepository())

        assertFailsWith<CartValidationException> {
            useCase.execute(0L)
        }
    }
}
