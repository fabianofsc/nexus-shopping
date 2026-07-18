package com.nexus.shopping.cart

import org.flywaydb.core.Flyway
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CartMigrationContractTest {
    @Test
    fun `carts and cart_items tables have FKs, check constraints, indexes and accept valid rows`() {
        val jdbcUrl = "jdbc:h2:mem:cart_migration_contract;DB_CLOSE_DELAY=-1"

        // Same H2 CHECK-constraint-session limitation documented in NotificationMigrationContractTest:
        // keep Flyway's migration and this test's statements on the exact same connection.
        val connection = DriverManager.getConnection(jdbcUrl, "sa", "")
        val singleConnectionDataSource = noCloseDataSource(connection)

        connection.use {
            Flyway
                .configure()
                .dataSource(singleConnectionDataSource)
                .locations("classpath:db/migration")
                .placeholders(mapOf("productSeedCount" to "10"))
                .load()
                .migrate()

            connection.createStatement().use { statement ->
                statement.executeUpdate("INSERT INTO carts (customer_id, status) VALUES (1, 'ACTIVE')")
            }
            assertEquals(1, countRows(connection, "carts"))

            val cartId =
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT id FROM carts WHERE customer_id = 1").use { rs ->
                        rs.next()
                        rs.getLong(1)
                    }
                }

            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    INSERT INTO cart_items (cart_id, product_id, product_name, unit_price_amount, currency, quantity)
                    VALUES ($cartId, 10, 'Product 10', 19.90, 'BRL', 2)
                    """.trimIndent(),
                )
            }
            assertEquals(1, countRows(connection, "cart_items WHERE cart_id = $cartId"))

            assertFailsWith<SQLException> {
                connection.createStatement().use { statement ->
                    statement.executeUpdate("INSERT INTO carts (customer_id, status) VALUES (999999, 'ACTIVE')")
                }
            }

            assertFailsWith<SQLException> {
                connection.createStatement().use { statement ->
                    statement.executeUpdate("INSERT INTO carts (customer_id, status) VALUES (1, 'NOT_A_REAL_STATUS')")
                }
            }

            assertFailsWith<SQLException> {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        INSERT INTO cart_items (cart_id, product_id, product_name, unit_price_amount, currency, quantity)
                        VALUES ($cartId, 20, 'Product 20', 9.90, 'BRL', 0)
                        """.trimIndent(),
                    )
                }
            }

            assertTrue(
                countRows(
                    connection,
                    "INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME = 'CARTS' AND CONSTRAINT_NAME = 'CARTS_STATUS_CHECK' AND CONSTRAINT_TYPE = 'CHECK'",
                ) >= 1,
            )
            assertTrue(
                countRows(
                    connection,
                    "INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'CARTS' AND INDEX_NAME = 'IDX_CARTS_CUSTOMER_ID_STATUS'",
                ) >= 1,
            )
            assertTrue(
                countRows(
                    connection,
                    "INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'CART_ITEMS' AND INDEX_NAME = 'IDX_CART_ITEMS_CART_ID'",
                ) >= 1,
            )
            assertTrue(
                countRows(
                    connection,
                    "INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                        "WHERE TABLE_NAME = 'CARTS' AND CONSTRAINT_NAME = 'FK_CARTS_CUSTOMER'",
                ) >= 1,
                "expected named FK constraint fk_carts_customer to exist",
            )
            assertTrue(
                countRows(
                    connection,
                    "INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                        "WHERE TABLE_NAME = 'CART_ITEMS' AND CONSTRAINT_NAME = 'FK_CART_ITEMS_CART'",
                ) >= 1,
                "expected named FK constraint fk_cart_items_cart to exist",
            )
        }
    }

    /** Wraps [connection] so `close()` is a no-op, and exposes it as a [DataSource] Flyway always reuses. */
    private fun noCloseDataSource(connection: Connection): DataSource {
        val noCloseConnection =
            Proxy.newProxyInstance(
                Connection::class.java.classLoader,
                arrayOf(Connection::class.java),
            ) { _, method, args ->
                if (method.name == "close") null else method.invoke(connection, *(args ?: emptyArray()))
            } as Connection

        return Proxy.newProxyInstance(
            DataSource::class.java.classLoader,
            arrayOf(DataSource::class.java),
        ) { _, method, _ ->
            if (method.name == "getConnection") noCloseConnection else null
        } as DataSource
    }

    private fun countRows(
        connection: Connection,
        fromClause: String,
    ): Int {
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM $fromClause").use { resultSet ->
                resultSet.next()
                return resultSet.getInt(1)
            }
        }
    }
}
