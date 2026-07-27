package com.nexus.shopping.cart.application.port.outbound

import com.nexus.shopping.cart.domain.Cart

interface CartRepositoryPort {
    fun findActiveByCustomerId(customerId: Long): Cart?

    /** Finds and locks the customer's ACTIVE cart for the duration of the checkout transaction. */
    fun reserveActiveCart(customerId: Long): Cart? = findActiveByCustomerId(customerId)

    /** Marks the cart represented by a checkout reservation as CHECKED_OUT. */
    fun confirmCheckout(reservationId: Long): Unit = error("Checkout confirmation is not supported by this repository.")

    /**
     * Returns the customer's ACTIVE cart, creating an empty one if none exists yet. Implementations
     * must guarantee this is atomic under concurrency: a customer may have at most one ACTIVE cart,
     * even when multiple requests race to create the first one for that customer at the same time.
     */
    fun getOrCreateActiveByCustomerId(customerId: Long): Cart

    /**
     * Finds the customer's latest cart for a mutation, creating an ACTIVE cart only when no cart
     * exists yet. Callers must still validate the status inside [updateCart], after the write lock
     * was acquired, because checkout may close the cart between this lookup and the mutation.
     */
    fun getOrCreateCartForMutationByCustomerId(customerId: Long): Cart = getOrCreateActiveByCustomerId(customerId)

    /**
     * Applies [mutate] to the cart identified by [cartId] and persists the result, guaranteeing the
     * read-modify-write cycle is atomic per cart: implementations must load the current state under
     * a write lock and evaluate [mutate] against that freshly-locked read, not against a snapshot
     * taken before the lock was acquired - otherwise concurrent calls (e.g. two "add item" requests
     * racing on the same cart) can each compute their new state from the same stale base and one
     * update silently overwrites the other (a lost update), even if no exception is ever thrown.
     */
    fun updateCart(
        cartId: Long,
        mutate: (Cart) -> Cart,
    ): Cart
}
