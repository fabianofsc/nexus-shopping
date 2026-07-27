package com.nexus.shopping.order.adapter.outbound.jpa

import com.nexus.shopping.cart.adapter.outbound.jpa.CartJpaRepositoryAdapter
import com.nexus.shopping.order.domain.Currency
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.OrderStatus
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:order_jpa_repository_adapter_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.generate_statistics=true",
    ],
)
@Transactional
class OrderJpaRepositoryAdapterTest {
    @Autowired
    private lateinit var orders: OrderJpaRepositoryAdapter

    @Autowired
    private lateinit var carts: CartJpaRepositoryAdapter

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Test
    fun `persists historical order snapshots and finds them by idempotency key`() {
        val cart = carts.getOrCreateActiveByCustomerId(1L)
        val requested = order(requireNotNull(cart.id))

        val created = orders.create(requested)
        val replay = orders.findByCustomerIdAndIdempotencyKey(1L, "checkout-1")
        val found = orders.findById(requireNotNull(created.order.id))

        assertEquals(created.order, replay)
        assertEquals(created.order, found)
        assertEquals("Ana Silva", found?.customerSnapshot?.name)
        assertEquals("Rua A", found?.shippingAddressSnapshot?.street)
        assertEquals(listOf(item()), found?.items)
    }

    @Test
    fun `duplicate direct insert returns the existing order without exposing a uniqueness violation`() {
        val cart = carts.getOrCreateActiveByCustomerId(3L)
        val requested = order(requireNotNull(cart.id), "checkout-3")
        val created = orders.create(requested)

        val replay = orders.create(requested)

        assertEquals(true, created.created)
        assertEquals(false, replay.created)
        assertEquals(created.order, replay.order)
    }

    @Test
    fun `finds by customer idempotency key and updates cancellation without replacing snapshots`() {
        val cart = carts.getOrCreateActiveByCustomerId(2L)
        val created = orders.create(order(requireNotNull(cart.id), "checkout-2")).order

        val cancelled = orders.update(created.cancel())
        val replay = orders.findByCustomerIdAndIdempotencyKey(2L, "checkout-2")

        assertNotNull(cancelled.cancelledAt)
        assertEquals(OrderStatus.CANCELLED, replay?.status)
        assertEquals(listOf(item()), replay?.items)
    }

    @Test
    fun `lists a page of orders without one item query per order`() {
        val cartIds =
            (1..3).map {
                val cart = carts.getOrCreateActiveByCustomerId(1L)
                carts.confirmCheckout(requireNotNull(cart.id))
                requireNotNull(cart.id)
            }
        cartIds.forEach { cartId ->
            orders.create(order(cartId, "batch-$cartId"))
        }

        entityManager.clear()
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.clear()

        val page = orders.findByCustomerId(customerId = 1L, page = 0, size = 10)

        assertEquals(3, page.content.size)
        assertTrue(
            statistics.prepareStatementCount <= 2,
            "Expected the order page and its items to use at most two statements, but used ${statistics.prepareStatementCount}.",
        )
    }

    private fun order(
        cartId: Long,
        idempotencyKey: String = "checkout-1",
    ) = Order(
        id = null,
        customerId =
            when (idempotencyKey) {
                "checkout-2" -> 2L
                "checkout-3" -> 3L
                else -> 1L
            },
        cartId = cartId,
        customerSnapshot =
            CustomerSnapshot(
                when (idempotencyKey) {
                    "checkout-2" -> 2L
                    "checkout-3" -> 3L
                    else -> 1L
                },
                "Ana Silva",
                "12345678900",
                "CPF",
                "ana@example.com",
                null,
            ),
        shippingAddressSnapshot = ShippingAddressSnapshot("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
        items = listOf(item()),
        status = OrderStatus.WAITING_PAYMENT,
        idempotencyKey = idempotencyKey,
        requestFingerprint = "a".repeat(64),
        createdAt = null,
        cancelledAt = null,
    )

    private fun item() = OrderItemSnapshot(10L, "Produto 10", BigDecimal("19.90"), Currency.BRL, 2)
}
