package com.nexus.shopping.cart

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.application.usecase.RemoveCartItemUseCase
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

private class FakeRemoveCartItemRepository(
    var activeCart: Cart? = null,
) : CartRepositoryPort {
    val savedCarts = mutableListOf<Cart>()

    override fun findActiveByCustomerId(customerId: Long): Cart? = activeCart

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

class RemoveCartItemUseCaseTest {
    private fun cartWithItems(vararg productIds: Long) =
        Cart(
            id = 1L,
            customerId = 1L,
            status = CartStatus.ACTIVE,
            items =
                productIds.map { productId ->
                    CartItem(
                        productSummary =
                            ProductSummary(
                                productId = productId,
                                name = "Product $productId",
                                unitPriceAmount = BigDecimal("19.90"),
                                currency = Currency.BRL,
                            ),
                        quantity = 1,
                    )
                },
            createdAt = Instant.parse("2026-07-17T12:00:00Z"),
            updatedAt = Instant.parse("2026-07-17T12:00:00Z"),
        )

    @Test
    fun `removes the matching item from the ACTIVE cart`() {
        val repository = FakeRemoveCartItemRepository(activeCart = cartWithItems(10L, 20L))
        val useCase = RemoveCartItemUseCase(repository)

        val cart = useCase.execute(customerId = 1L, productId = 10L)

        assertEquals(1, cart.items.size)
        assertEquals(20L, cart.items[0].productSummary.productId)
    }

    @Test
    fun `is a no-op and creates an empty cart when no ACTIVE cart exists`() {
        val repository = FakeRemoveCartItemRepository()
        val useCase = RemoveCartItemUseCase(repository)

        val cart = useCase.execute(customerId = 1L, productId = 10L)

        assertEquals(emptyList(), cart.items)
    }

    @Test
    fun `throws CartValidationException when customerId is not positive`() {
        val useCase = RemoveCartItemUseCase(FakeRemoveCartItemRepository())

        assertFailsWith<CartValidationException> {
            useCase.execute(customerId = 0L, productId = 10L)
        }
    }

    @Test
    fun `throws CartValidationException when productId is not positive`() {
        val useCase = RemoveCartItemUseCase(FakeRemoveCartItemRepository())

        assertFailsWith<CartValidationException> {
            useCase.execute(customerId = 1L, productId = 0L)
        }
    }
}
