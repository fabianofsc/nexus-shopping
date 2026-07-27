package com.nexus.shopping.cart

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.application.usecase.CartCheckoutUseCase
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

private class CheckoutCartRepositoryFake(
    var activeCart: Cart?,
) : CartRepositoryPort {
    var confirmedReservationId: Long? = null

    override fun findActiveByCustomerId(customerId: Long): Cart? = activeCart?.takeIf { it.customerId == customerId }

    override fun getOrCreateActiveByCustomerId(customerId: Long): Cart = error("Not used by checkout")

    override fun updateCart(
        cartId: Long,
        mutate: (Cart) -> Cart,
    ): Cart = error("Not used by checkout")

    override fun reserveActiveCart(customerId: Long): Cart? = findActiveByCustomerId(customerId)

    override fun confirmCheckout(reservationId: Long) {
        confirmedReservationId = reservationId
    }
}

class CartCheckoutUseCaseTest {
    private fun activeCart(items: List<CartItem> = listOf(item())) =
        Cart(
            id = 100L,
            customerId = 10L,
            status = CartStatus.ACTIVE,
            items = items,
            createdAt = Instant.parse("2026-07-26T12:00:00Z"),
            updatedAt = Instant.parse("2026-07-26T12:00:00Z"),
        )

    private fun item() =
        CartItem(
            ProductSummary(1L, "Produto A", BigDecimal("19.90"), Currency.BRL),
            quantity = 2,
        )

    @Test
    fun `reserves the customer's ACTIVE cart as a Cart reservation`() {
        val checkout = CartCheckoutUseCase(CheckoutCartRepositoryFake(activeCart()))

        val reservation = checkout.reserveActiveCart(10L)

        assertEquals(100L, reservation.id)
        assertEquals(10L, reservation.customerId)
        assertEquals(listOf(item()), reservation.items)
    }

    @Test
    fun `keeps an empty ACTIVE cart reserved so the caller can reject its own empty input`() {
        val checkout = CartCheckoutUseCase(CheckoutCartRepositoryFake(activeCart(emptyList())))

        val reservation = checkout.reserveActiveCart(10L)

        assertEquals(emptyList(), reservation.items)
    }

    @Test
    fun `rejects a checkout reservation when the customer has no ACTIVE cart`() {
        val checkout = CartCheckoutUseCase(CheckoutCartRepositoryFake(activeCart()))

        assertFailsWith<CartValidationException> {
            checkout.reserveActiveCart(20L)
        }
    }

    @Test
    fun `confirms checkout using the reservation identifier`() {
        val repository = CheckoutCartRepositoryFake(activeCart())

        CartCheckoutUseCase(repository).confirmCheckout(100L)

        assertEquals(100L, repository.confirmedReservationId)
    }
}
