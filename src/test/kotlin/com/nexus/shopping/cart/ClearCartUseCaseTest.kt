package com.nexus.shopping.cart

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.application.usecase.ClearCartUseCase
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

private class FakeClearCartRepository(
    var activeCart: Cart? = null,
) : CartRepositoryPort {
    val savedCarts = mutableListOf<Cart>()

    override fun findActiveByCustomerId(customerId: Long): Cart? = activeCart

    override fun reserveActiveCart(customerId: Long): Cart? = findActiveByCustomerId(customerId)

    override fun confirmCheckout(reservationId: Long) = error("Not used by clear cart")

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
        val persisted = cart.copy(id = cart.id ?: 1L, updatedAt = Instant.parse("2026-07-17T12:00:00Z"))
        savedCarts += persisted
        activeCart = persisted
        return persisted
    }
}

class ClearCartUseCaseTest {
    @Test
    fun `removes all items but keeps the cart ACTIVE`() {
        val cart =
            Cart(
                id = 1L,
                customerId = 1L,
                status = CartStatus.ACTIVE,
                items =
                    listOf(
                        CartItem(
                            productSummary =
                                ProductSummary(
                                    productId = 10L,
                                    name = "Product 10",
                                    unitPriceAmount = BigDecimal("19.90"),
                                    currency = Currency.BRL,
                                ),
                            quantity = 2,
                        ),
                    ),
                createdAt = Instant.parse("2026-07-17T12:00:00Z"),
                updatedAt = Instant.parse("2026-07-17T12:00:00Z"),
            )
        val repository = FakeClearCartRepository(activeCart = cart)
        val useCase = ClearCartUseCase(repository)

        val result = useCase.execute(customerId = 1L)

        assertEquals(emptyList(), result.items)
        assertEquals(CartStatus.ACTIVE, result.status)
    }

    @Test
    fun `is a no-op and creates an empty cart when no ACTIVE cart exists`() {
        val repository = FakeClearCartRepository()
        val useCase = ClearCartUseCase(repository)

        val result = useCase.execute(customerId = 1L)

        assertEquals(emptyList(), result.items)
    }

    @Test
    fun `throws CartValidationException when customerId is not positive`() {
        val useCase = ClearCartUseCase(FakeClearCartRepository())

        assertFailsWith<CartValidationException> {
            useCase.execute(customerId = 0L)
        }
    }
}
