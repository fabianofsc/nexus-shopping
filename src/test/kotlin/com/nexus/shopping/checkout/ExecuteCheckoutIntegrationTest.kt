package com.nexus.shopping.checkout

import com.nexus.shopping.checkout.application.model.AppliedOrderPaymentResult
import com.nexus.shopping.checkout.application.model.ApplyOrderPaymentResultByReferenceCommand
import com.nexus.shopping.checkout.application.model.ApplyOrderPaymentResultCommand
import com.nexus.shopping.checkout.application.model.CheckoutCartSnapshot
import com.nexus.shopping.checkout.application.model.CheckoutCommand
import com.nexus.shopping.checkout.application.model.EnsureOrderConfirmationCommand
import com.nexus.shopping.checkout.application.model.PaymentAuthorizationCommand
import com.nexus.shopping.checkout.application.model.PaymentProcessingCommand
import com.nexus.shopping.checkout.application.model.PaymentProcessingResult
import com.nexus.shopping.checkout.application.model.PaymentResultStatus
import com.nexus.shopping.checkout.application.model.PaymentValidationCommand
import com.nexus.shopping.checkout.application.port.outbound.CheckoutCartGateway
import com.nexus.shopping.checkout.application.port.outbound.CheckoutCustomerGateway
import com.nexus.shopping.checkout.application.port.outbound.InventoryGateway
import com.nexus.shopping.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.checkout.application.port.outbound.OrderCreationGateway
import com.nexus.shopping.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentAuthorizationFingerprintGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentProcessingGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentValidationGateway
import com.nexus.shopping.checkout.application.port.outbound.TransactionPort
import com.nexus.shopping.checkout.application.usecase.ExecuteCheckoutUseCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:checkout_workflow_integration_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class ExecuteCheckoutIntegrationTest {
    @Autowired
    private lateinit var carts: CheckoutCartGateway

    @Autowired
    private lateinit var orders: OrderCreationGateway

    @Autowired
    private lateinit var transaction: TransactionPort

    @Autowired
    private lateinit var inventory: InventoryGateway

    @Autowired
    private lateinit var customers: CheckoutCustomerGateway

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `rolls back Cart and Order in H2 when checkout fails after confirmation`() {
        val customerId = 6L
        val cartId = prepareActiveCart(customerId)
        val failure = IllegalStateException("failure after Cart confirmation")
        val failingCarts =
            object : CheckoutCartGateway {
                override fun reserveActiveCart(customerId: Long): CheckoutCartSnapshot = carts.reserveActiveCart(customerId)

                override fun confirmCheckout(reservationId: Long) {
                    carts.confirmCheckout(reservationId)
                    throw failure
                }
            }
        val checkout = checkout(failingCarts)

        assertCheckoutRolledBack(cartId, failure) {
            checkout.execute(command(customerId))
        }
    }

    @Test
    fun `participates in an existing REQUIRED transaction`() {
        val customerId = 7L
        val cartId = prepareActiveCart(customerId)
        val failure = IllegalStateException("failure in outer transaction")
        val checkout = checkout(carts)
        val outerTransaction = TransactionTemplate(transactionManager)

        assertCheckoutRolledBack(cartId, failure) {
            outerTransaction.executeWithoutResult {
                checkout.execute(command(customerId))
                throw failure
            }
        }
    }

    private fun assertCheckoutRolledBack(
        cartId: Long,
        failure: IllegalStateException,
        checkout: () -> Unit,
    ) {
        val thrown = assertFailsWith<IllegalStateException>(block = checkout)
        assertSame(failure, thrown)
        assertEquals(
            "ACTIVE",
            jdbcTemplate.queryForObject("SELECT status FROM carts WHERE id = ?", String::class.java, cartId),
        )
        assertEquals(
            0,
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE cart_id = ?", Int::class.java, cartId),
        )
    }

    private fun prepareActiveCart(customerId: Long): Long {
        jdbcTemplate.seedStockedProduct()
        jdbcTemplate.update("INSERT INTO carts (customer_id, status) VALUES (?, 'ACTIVE')", customerId)
        val cartId =
            requireNotNull(
                jdbcTemplate.queryForObject(
                    "SELECT MAX(id) FROM carts WHERE customer_id = ?",
                    Long::class.java,
                    customerId,
                ),
            )
        jdbcTemplate.update(
            """
            INSERT INTO cart_items (cart_id, product_id, product_name, unit_price_amount, currency, quantity)
            VALUES (?, 10, 'Produto 10', 19.90, 'BRL', 2)
            """.trimIndent(),
            cartId,
        )
        return cartId
    }

    private fun command(customerId: Long) =
        CheckoutCommand(
            customerId = customerId,
            paymentToken = "approved",
            idempotencyKey = "rollback-checkout-$customerId",
        )

    private fun checkout(cartGateway: CheckoutCartGateway) =
        ExecuteCheckoutUseCase(
            carts = cartGateway,
            customers = customers,
            orders = orders,
            paymentAuthorizationFingerprints =
                object : PaymentAuthorizationFingerprintGateway {
                    override fun fingerprint(command: PaymentAuthorizationCommand) = "opaque-payment-authorization-fingerprint"
                },
            paymentValidation =
                object : PaymentValidationGateway {
                    override fun validate(command: PaymentValidationCommand) = Unit
                },
            payments =
                object : PaymentProcessingGateway {
                    override fun process(command: PaymentProcessingCommand) =
                        PaymentProcessingResult("pay-requested", PaymentResultStatus.REQUESTED, null, replayed = false)
                },
            orderPaymentResults =
                object : OrderPaymentResultGateway {
                    override fun apply(command: ApplyOrderPaymentResultCommand) = error("Not used")

                    override fun applyByOrderReference(command: ApplyOrderPaymentResultByReferenceCommand): AppliedOrderPaymentResult =
                        error("Not used")
                },
            notifications =
                object : NotificationGateway {
                    override fun ensureOrderConfirmation(command: EnsureOrderConfirmationCommand) = error("Not used")
                },
            inventory = inventory,
            transaction = transaction,
        )
}
