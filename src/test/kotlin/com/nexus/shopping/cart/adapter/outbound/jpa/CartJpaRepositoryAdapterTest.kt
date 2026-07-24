package com.nexus.shopping.cart.adapter.outbound.jpa

import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.domain.CartItem
import com.nexus.shopping.cart.domain.CartStatus
import com.nexus.shopping.cart.domain.Currency
import com.nexus.shopping.cart.domain.ProductSummary
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:cart_jpa_repository_adapter_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@Transactional
class CartJpaRepositoryAdapterTest {
    @Autowired
    private lateinit var repository: CartJpaRepositoryAdapter

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var springDataRepository: SpringDataCartRepository

    private fun item(
        productId: Long,
        quantity: Int,
    ) = CartItem(
        productSummary =
            ProductSummary(
                productId = productId,
                name = "Product $productId",
                unitPriceAmount = BigDecimal("19.90"),
                currency = Currency.BRL,
            ),
        quantity = quantity,
    )

    @Test
    fun `findActiveByCustomerId returns null when no ACTIVE cart exists`() {
        assertNull(repository.findActiveByCustomerId(2L))
    }

    @Test
    fun `getOrCreateActiveByCustomerId creates a new empty ACTIVE cart when none exists`() {
        val cart = repository.getOrCreateActiveByCustomerId(1L)

        assertEquals(1L, cart.customerId)
        assertEquals(CartStatus.ACTIVE, cart.status)
        assertEquals(emptyList(), cart.items)
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM carts WHERE customer_id = 1", Int::class.java))
    }

    @Test
    fun `getOrCreateActiveByCustomerId returns the existing ACTIVE cart without creating a duplicate`() {
        val first = repository.getOrCreateActiveByCustomerId(3L)

        val second = repository.getOrCreateActiveByCustomerId(3L)

        assertEquals(first.id, second.id)
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM carts WHERE customer_id = 3", Int::class.java))
    }

    @Test
    fun `getOrCreateActiveByCustomerId throws CartValidationException when customerId does not reference an existing customer`() {
        assertFailsWith<CartValidationException> {
            repository.getOrCreateActiveByCustomerId(999999L)
        }
    }

    @Test
    fun `updateCart updates items on an existing cart`() {
        val cart = repository.getOrCreateActiveByCustomerId(4L)

        val saved = repository.updateCart(requireNotNull(cart.id)) { it.copy(items = listOf(item(10L, 2))) }
        val found = repository.findActiveByCustomerId(4L)

        assertEquals(cart.id, saved.id)
        assertEquals(1, found?.items?.size)
        assertEquals(
            10L,
            found
                ?.items
                ?.get(0)
                ?.productSummary
                ?.productId,
        )
        assertEquals(2, found?.items?.get(0)?.quantity)
    }

    @Test
    fun `updateCart reconciles items - adds, updates and removes rows`() {
        val cart = repository.getOrCreateActiveByCustomerId(5L)
        repository.updateCart(requireNotNull(cart.id)) { it.copy(items = listOf(item(10L, 1), item(20L, 1))) }

        val updated =
            repository.updateCart(requireNotNull(cart.id)) {
                it.copy(
                    items =
                        listOf(
                            item(10L, 5),
                            item(30L, 1),
                        ),
                )
            }

        assertEquals(2, updated.items.size)
        val byProductId = updated.items.associateBy { it.productSummary.productId }
        assertEquals(5, byProductId.getValue(10L).quantity)
        assertEquals(1, byProductId.getValue(30L).quantity)
        assertEquals(null, byProductId[20L])
        assertEquals(
            2,
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cart_items WHERE cart_id = ${updated.id}", Int::class.java),
        )
    }

    @Test
    fun `updateCart applies the mutation against the freshly loaded cart, not a stale snapshot passed in by the caller`() {
        val cart = repository.getOrCreateActiveByCustomerId(6L)
        // Simulate another concurrent writer having already committed an item, without going
        // through the same in-memory snapshot: `cart` above still has an empty items list.
        repository.updateCart(requireNotNull(cart.id)) { it.withItemAdded(item(10L, 1).productSummary, 1) }

        val result = repository.updateCart(requireNotNull(cart.id)) { it.withItemAdded(item(20L, 1).productSummary, 1) }

        // If the mutation had been evaluated against the stale `cart` snapshot (empty items)
        // instead of a fresh read, productId 10 would have been lost here.
        assertEquals(2, result.items.size)
        val productIds = result.items.map { it.productSummary.productId }.toSet()
        assertEquals(setOf(10L, 20L), productIds)
    }

    @Test
    fun `latest cart query returns a one-row slice instead of materializing customer history`() {
        jdbcTemplate.update("INSERT INTO carts (customer_id, status) VALUES (7, 'CHECKED_OUT')")
        jdbcTemplate.update("INSERT INTO carts (customer_id, status) VALUES (7, 'ACTIVE')")

        val slice = springDataRepository.findLatestByCustomerId(7L, PageRequest.of(0, 1))

        assertEquals(1, slice.content.size)
        assertEquals(CartStatus.ACTIVE, slice.content.single().status)
    }
}
