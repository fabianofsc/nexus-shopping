package com.nexus.shopping.cart.adapter.outbound.jpa

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartItem
import com.nexus.shopping.cart.domain.CartStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class CartJpaRepositoryAdapter(
    private val repository: SpringDataCartRepository,
) : CartRepositoryPort {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Transactional(readOnly = true)
    override fun findActiveByCustomerId(customerId: Long): Cart? =
        repository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE).orElse(null)?.toDomain()

    /**
     * Locks the customer's own row (`SELECT ... FOR UPDATE`, portable between H2 and PostgreSQL)
     * before checking for / creating the ACTIVE cart, so concurrent requests for the same
     * customerId serialize instead of racing: the first caller to acquire the lock creates the
     * cart and holds the lock until commit; every other concurrent caller blocks on that same row
     * lock, and by the time it proceeds, the lookup below already sees the committed row and
     * returns it instead of inserting a duplicate ACTIVE cart. There is no row on `carts` itself
     * to lock the first time this runs for a customer (that is the whole problem), so the lock is
     * anchored on `customers` instead, which is guaranteed to already exist for a valid customerId.
     *
     * The lock query's own result also tells us, deterministically, whether customerId exists at
     * all: an empty result means no such customer, so we fail fast with CartValidationException
     * here instead of attempting an INSERT that is guaranteed to fail on the fk_carts_customer
     * constraint - detecting the error this way does not depend on matching a constraint name
     * against a specific driver/dialect's exception message.
     */
    @Transactional
    override fun getOrCreateActiveByCustomerId(customerId: Long): Cart {
        if (!lockCustomerRow(customerId)) {
            throw CartValidationException("customerId $customerId does not reference an existing customer.")
        }

        findActiveByCustomerId(customerId)?.let { return it }

        val newCart =
            Cart(
                id = null,
                customerId = customerId,
                status = CartStatus.ACTIVE,
                items = emptyList(),
                createdAt = null,
                updatedAt = null,
            )
        return repository.saveAndFlush(newCart.toNewEntity()).toDomain()
    }

    @Transactional
    override fun getOrCreateCartForMutationByCustomerId(customerId: Long): Cart {
        if (!lockCustomerRow(customerId)) {
            throw CartValidationException("customerId $customerId does not reference an existing customer.")
        }

        repository
            .findLatestByCustomerId(customerId, PageRequest.of(0, 1))
            .content
            .firstOrNull()
            ?.let { return it.toDomain() }

        val newCart =
            Cart(
                id = null,
                customerId = customerId,
                status = CartStatus.ACTIVE,
                items = emptyList(),
                createdAt = null,
                updatedAt = null,
            )
        return repository.saveAndFlush(newCart.toNewEntity()).toDomain()
    }

    /**
     * Loads the cart under a pessimistic write lock (`findByIdForUpdate`) and evaluates [mutate]
     * against that freshly-locked read - not against whatever snapshot the caller may have read
     * earlier - so this whole read-modify-write cycle is atomic per cart: two concurrent calls
     * (e.g. two "add item" requests, or an add racing a remove) can no longer both compute their
     * new state from the same stale base. The second caller blocks until the first commits, then
     * [mutate] runs again against the already-committed state, so no update is silently lost.
     */
    @Transactional
    override fun updateCart(
        cartId: Long,
        mutate: (Cart) -> Cart,
    ): Cart {
        val entity = repository.findByIdForUpdate(cartId).orElseThrow { IllegalStateException("Cart $cartId not found.") }
        val mutated = mutate(entity.toDomain())
        entity.status = mutated.status
        reconcileItems(entity, mutated.items)
        return repository.saveAndFlush(entity).toDomain()
    }

    @Transactional
    override fun reserveActiveCart(customerId: Long): Cart? =
        repository
            .findByCustomerIdAndStatusForUpdate(customerId, CartStatus.ACTIVE)
            .orElse(null)
            ?.toDomain()

    @Transactional
    override fun confirmCheckout(reservationId: Long) {
        val entity =
            repository.findByIdForUpdate(reservationId).orElseThrow {
                IllegalStateException("Cart $reservationId not found.")
            }
        entity.status = CartStatus.CHECKED_OUT
        repository.saveAndFlush(entity)
    }

    /** Returns true if customerId references an existing row, having taken a write lock on it. */
    private fun lockCustomerRow(customerId: Long): Boolean =
        entityManager
            .createNativeQuery("SELECT id FROM customers WHERE id = ? FOR UPDATE")
            .setParameter(1, customerId)
            .resultList
            .isNotEmpty()

    private fun reconcileItems(
        entity: CartEntity,
        desiredItems: List<CartItem>,
    ) {
        val desiredByProductId = desiredItems.associateBy { it.productSummary.productId }
        entity.items.removeAll { it.productId !in desiredByProductId.keys }

        val existingByProductId = entity.items.associateBy { it.productId }
        desiredByProductId.forEach { (productId, item) ->
            val existing = existingByProductId[productId]
            if (existing != null) {
                existing.productName = item.productSummary.name
                existing.unitPriceAmount = item.productSummary.unitPriceAmount
                existing.currency = item.productSummary.currency
                existing.quantity = item.quantity
            } else {
                entity.items.add(
                    CartItemEntity(
                        cart = entity,
                        productId = productId,
                        productName = item.productSummary.name,
                        unitPriceAmount = item.productSummary.unitPriceAmount,
                        currency = item.productSummary.currency,
                        quantity = item.quantity,
                    ),
                )
            }
        }
    }
}
