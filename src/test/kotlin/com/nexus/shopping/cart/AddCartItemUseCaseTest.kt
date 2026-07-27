package com.nexus.shopping.cart

import com.nexus.shopping.cart.application.command.AddCartItemCommand
import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.application.usecase.AddCartItemUseCase
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartStatus
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeAddCartItemRepository(
    var activeCart: Cart? = null,
) : CartRepositoryPort {
    val savedCarts = mutableListOf<Cart>()
    private var nextId = 1L

    override fun findActiveByCustomerId(customerId: Long): Cart? = activeCart

    override fun reserveActiveCart(customerId: Long): Cart? = findActiveByCustomerId(customerId)

    override fun confirmCheckout(reservationId: Long) = error("Not used by add item")

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

class AddCartItemUseCaseTest {
    private fun validCommand() =
        AddCartItemCommand(
            customerId = 1L,
            productId = 10L,
            productName = "Product 10",
            unitPriceAmount = BigDecimal("19.90"),
            currency = "BRL",
            quantity = 2,
        )

    @Test
    fun `adds a new item and creates the cart when none exists`() {
        val repository = FakeAddCartItemRepository()
        val useCase = AddCartItemUseCase(repository)

        val cart = useCase.execute(validCommand())

        assertEquals(1, cart.items.size)
        assertEquals(10L, cart.items[0].productSummary.productId)
        assertEquals(2, cart.items[0].quantity)
    }

    @Test
    fun `sums quantity and replaces product summary when productId already exists in the ACTIVE cart`() {
        val repository = FakeAddCartItemRepository()
        val useCase = AddCartItemUseCase(repository)
        useCase.execute(validCommand())

        val cart =
            useCase.execute(
                validCommand().copy(
                    productName = "Product 10 updated",
                    unitPriceAmount = BigDecimal("24.90"),
                    quantity = 3,
                ),
            )

        assertEquals(1, cart.items.size)
        assertEquals(5, cart.items[0].quantity)
        assertEquals("Product 10 updated", cart.items[0].productSummary.name)
        assertEquals(BigDecimal("24.90"), cart.items[0].productSummary.unitPriceAmount)
    }

    @Test
    fun `throws CartValidationException when customerId is not positive`() {
        val useCase = AddCartItemUseCase(FakeAddCartItemRepository())

        assertFailsWith<CartValidationException> {
            useCase.execute(validCommand().copy(customerId = 0L))
        }
    }

    @Test
    fun `throws CartValidationException when productId is not positive`() {
        val useCase = AddCartItemUseCase(FakeAddCartItemRepository())

        assertFailsWith<CartValidationException> {
            useCase.execute(validCommand().copy(productId = 0L))
        }
    }

    @Test
    fun `throws CartValidationException when quantity is not positive`() {
        val useCase = AddCartItemUseCase(FakeAddCartItemRepository())

        assertFailsWith<CartValidationException> {
            useCase.execute(validCommand().copy(quantity = 0))
        }
    }

    @Test
    fun `throws CartValidationException when unitPriceAmount is negative`() {
        val useCase = AddCartItemUseCase(FakeAddCartItemRepository())

        assertFailsWith<CartValidationException> {
            useCase.execute(validCommand().copy(unitPriceAmount = BigDecimal("-0.01")))
        }
    }

    @Test
    fun `throws CartValidationException when currency is not a valid enum value`() {
        val useCase = AddCartItemUseCase(FakeAddCartItemRepository())

        assertFailsWith<CartValidationException> {
            useCase.execute(validCommand().copy(currency = "XYZ"))
        }
    }

    @Test
    fun `throws CartValidationException when productName is blank`() {
        val useCase = AddCartItemUseCase(FakeAddCartItemRepository())

        assertFailsWith<CartValidationException> {
            useCase.execute(validCommand().copy(productName = " "))
        }
    }
}
