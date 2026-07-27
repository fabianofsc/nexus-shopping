package com.nexus.shopping.integration.checkout

import com.nexus.shopping.integration.checkout.application.CheckoutWorkflowUseCase
import com.nexus.shopping.integration.checkout.application.model.CheckoutCartData
import com.nexus.shopping.integration.checkout.application.model.CheckoutCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutCustomerData
import com.nexus.shopping.integration.checkout.application.model.CheckoutShippingAddressData
import com.nexus.shopping.integration.checkout.application.port.outbound.CheckoutCartGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderCreationGateway
import com.nexus.shopping.integration.checkout.application.port.outbound.TransactionPort
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
class CheckoutWorkflowIntegrationTest {
    @Autowired
    private lateinit var carts: CheckoutCartGateway

    @Autowired
    private lateinit var orders: OrderCreationGateway

    @Autowired
    private lateinit var transaction: TransactionPort

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
                override fun reserveActiveCart(customerId: Long): CheckoutCartData = carts.reserveActiveCart(customerId)

                override fun confirmCheckout(reservationId: Long) {
                    carts.confirmCheckout(reservationId)
                    throw failure
                }
            }
        val checkout = CheckoutWorkflowUseCase(failingCarts, orders, transaction)

        assertCheckoutRolledBack(cartId, failure) {
            checkout.execute(command(customerId))
        }
    }

    @Test
    fun `participates in an existing REQUIRED transaction`() {
        val customerId = 7L
        val cartId = prepareActiveCart(customerId)
        val failure = IllegalStateException("failure in outer transaction")
        val checkout = CheckoutWorkflowUseCase(carts, orders, transaction)
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
            customerSnapshot =
                CheckoutCustomerData(
                    customerId,
                    "Claudia Elaine Eloa Galvao",
                    "378149714",
                    "RG",
                    "claudiaelainegalvao@athos.srv.br",
                    "+5579995737583",
                ),
            shippingAddressSnapshot =
                CheckoutShippingAddressData(
                    "Rua Rafael de Aguiar",
                    "557",
                    null,
                    "Pereira Lobo",
                    "Aracaju",
                    "SE",
                    "49052220",
                    "BR",
                ),
            idempotencyKey = "rollback-checkout-$customerId",
        )
}
