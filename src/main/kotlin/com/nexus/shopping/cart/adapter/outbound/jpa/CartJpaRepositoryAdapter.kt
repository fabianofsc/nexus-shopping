package com.nexus.shopping.cart.adapter.outbound.jpa

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartItem
import com.nexus.shopping.cart.domain.CartStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
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
     */
    @Transactional
    override fun getOrCreateActiveByCustomerId(customerId: Long): Cart {
        lockCustomerRow(customerId)

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
        return try {
            repository.saveAndFlush(newCart.toNewEntity()).toDomain()
        } catch (exception: DataIntegrityViolationException) {
            val violatedConstraint = (exception.cause as? ConstraintViolationException)?.constraintName
            if (violatedConstraint != null && violatedConstraint.contains("fk_carts_customer", ignoreCase = true)) {
                throw CartValidationException("customerId $customerId does not reference an existing customer.")
            }
            throw exception
        }
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

    private fun lockCustomerRow(customerId: Long) {
        entityManager
            .createNativeQuery("SELECT id FROM customers WHERE id = ? FOR UPDATE")
            .setParameter(1, customerId)
            .resultList
    }

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
