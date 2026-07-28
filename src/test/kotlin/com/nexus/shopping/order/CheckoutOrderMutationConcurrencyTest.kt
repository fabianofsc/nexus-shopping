package com.nexus.shopping.order

import com.nexus.shopping.cart.adapter.outbound.jpa.CartJpaRepositoryAdapter
import com.nexus.shopping.cart.application.command.AddCartItemCommand
import com.nexus.shopping.cart.application.exception.CartValidationException
import com.nexus.shopping.cart.application.port.inbound.CartCheckoutInputPort
import com.nexus.shopping.cart.application.port.inbound.CartCheckoutReservation
import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import com.nexus.shopping.cart.application.usecase.AddCartItemUseCase
import com.nexus.shopping.cart.application.usecase.CartCheckoutUseCase
import com.nexus.shopping.cart.application.usecase.ClearCartUseCase
import com.nexus.shopping.cart.application.usecase.RemoveCartItemUseCase
import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartItem
import com.nexus.shopping.cart.domain.ProductSummary
import com.nexus.shopping.integration.checkout.adapter.outbound.CheckoutJpaTransactionAdapter
import com.nexus.shopping.integration.checkout.adapter.outbound.local.LocalCheckoutCartGateway
import com.nexus.shopping.integration.checkout.adapter.outbound.local.LocalOrderCreationGateway
import com.nexus.shopping.integration.checkout.application.CheckoutWorkflowUseCase
import com.nexus.shopping.integration.checkout.application.model.CheckoutCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutCustomerData
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData
import com.nexus.shopping.integration.checkout.application.model.CheckoutShippingAddressData
import com.nexus.shopping.integration.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentProcessingGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.PaymentValidationGateway
import com.nexus.shopping.order.adapter.outbound.jpa.OrderJpaRepositoryAdapter
import com.nexus.shopping.order.application.usecase.CreateOrderUseCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import com.nexus.shopping.cart.domain.Currency as CartCurrency

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:order_checkout_mutation_concurrency_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class CheckoutOrderMutationConcurrencyTest {
    @Autowired
    private lateinit var orders: OrderJpaRepositoryAdapter

    @Autowired
    private lateinit var carts: CartJpaRepositoryAdapter

    @Autowired
    private lateinit var transactions: CheckoutJpaTransactionAdapter

    @Autowired
    private lateinit var paymentValidation: PaymentValidationGateway

    @Autowired
    private lateinit var payments: PaymentProcessingGateway

    @Autowired
    private lateinit var orderPaymentResults: OrderPaymentResultGateway

    @Autowired
    private lateinit var notifications: NotificationGateway

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `checkout lock rejects concurrent add after its stale ACTIVE read`() =
        assertCheckoutWinsAgainstMutation(5L) { repository ->
            AddCartItemUseCase(repository).execute(
                AddCartItemCommand(5L, 20L, "Produto 20", BigDecimal("9.90"), "BRL", 1),
            )
        }

    @Test
    fun `checkout lock rejects concurrent remove after its stale ACTIVE read`() =
        assertCheckoutWinsAgainstMutation(6L) { repository ->
            RemoveCartItemUseCase(repository).execute(6L, 10L)
        }

    @Test
    fun `checkout lock rejects concurrent clear after its stale ACTIVE read`() =
        assertCheckoutWinsAgainstMutation(7L) { repository ->
            ClearCartUseCase(repository).execute(7L)
        }

    private fun assertCheckoutWinsAgainstMutation(
        customerId: Long,
        mutation: (CartRepositoryPort) -> Cart,
    ) {
        prepareCart(customerId)
        val checkoutLocked = CountDownLatch(1)
        val releaseCheckout = CountDownLatch(1)
        val mutationReadActiveCart = CountDownLatch(1)
        val checkout =
            CreateOrderUseCase(orders).let { orderUseCase ->
                CheckoutWorkflowUseCase(
                    carts =
                        LocalCheckoutCartGateway(
                            BlockingCartCheckout(CartCheckoutUseCase(carts), checkoutLocked, releaseCheckout),
                        ),
                    orders = LocalOrderCreationGateway(orderUseCase, orderUseCase),
                    paymentValidation = paymentValidation,
                    payments = payments,
                    orderPaymentResults = orderPaymentResults,
                    notifications = notifications,
                    transaction = transactions,
                )
            }
        val signalingRepository = SignalingCartRepository(carts, mutationReadActiveCart)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val checkoutFuture = executor.submit<CheckoutOrderData> { checkout.execute(command(customerId, "checkout-$customerId")) }
            assertTrue(checkoutLocked.await(10, TimeUnit.SECONDS), "Checkout did not acquire the cart lock")

            val mutationFuture = executor.submit<Cart> { mutation(signalingRepository) }
            assertTrue(mutationReadActiveCart.await(10, TimeUnit.SECONDS), "Mutation did not read the ACTIVE cart")

            releaseCheckout.countDown()
            checkoutFuture.get(20, TimeUnit.SECONDS)
            val exception = assertFailsWith<ExecutionException> { mutationFuture.get(20, TimeUnit.SECONDS) }
            assertTrue(exception.cause is CartValidationException)
        } finally {
            releaseCheckout.countDown()
            executor.shutdownNow()
        }

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE customer_id = ?", Int::class.java, customerId))
        assertEquals(
            "CHECKED_OUT",
            jdbcTemplate.queryForObject("SELECT status FROM carts WHERE customer_id = ?", String::class.java, customerId),
        )
        assertEquals(
            1,
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cart_items ci JOIN carts c ON c.id = ci.cart_id WHERE c.customer_id = ?",
                Int::class.java,
                customerId,
            ),
        )
    }

    private fun prepareCart(customerId: Long) {
        val cart = carts.getOrCreateActiveByCustomerId(customerId)
        carts.updateCart(requireNotNull(cart.id)) {
            it.copy(
                items =
                    listOf(
                        CartItem(ProductSummary(10L, "Produto 10", BigDecimal("19.90"), CartCurrency.BRL), quantity = 2),
                    ),
            )
        }
    }

    private fun command(
        customerId: Long,
        idempotencyKey: String,
    ) = CheckoutCommand(
        customerId = customerId,
        customerSnapshot = CheckoutCustomerData(customerId, "Ana Silva", "12345678900", "CPF", "ana@example.com", null),
        shippingAddressSnapshot =
            CheckoutShippingAddressData("Rua A", "10", null, "Centro", "Sao Paulo", "SP", "01000-000", "BR"),
        paymentToken = "approved",
        idempotencyKey = idempotencyKey,
    )
}

private class BlockingCartCheckout(
    private val delegate: CartCheckoutInputPort,
    private val checkoutLocked: CountDownLatch,
    private val releaseCheckout: CountDownLatch,
) : CartCheckoutInputPort {
    override fun reserveActiveCart(customerId: Long): CartCheckoutReservation =
        delegate.reserveActiveCart(customerId).also {
            checkoutLocked.countDown()
            check(releaseCheckout.await(20, TimeUnit.SECONDS)) { "Timed out waiting to release checkout" }
        }

    override fun confirmCheckout(reservationId: Long) = delegate.confirmCheckout(reservationId)
}

private class SignalingCartRepository(
    private val delegate: CartRepositoryPort,
    private val mutationReadActiveCart: CountDownLatch,
) : CartRepositoryPort by delegate {
    override fun getOrCreateCartForMutationByCustomerId(customerId: Long): Cart =
        delegate.getOrCreateCartForMutationByCustomerId(customerId).also {
            mutationReadActiveCart.countDown()
        }
}
