package com.nexus.shopping.order

import org.flywaydb.core.Flyway
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OrderMigrationContractTest {
    @Test
    fun `orders and order_items preserve snapshots and enforce checkout uniqueness`() {
        DriverManager.getConnection("jdbc:h2:mem:order_migration_contract;DB_CLOSE_DELAY=-1", "sa", "").use { connection ->
            Flyway
                .configure()
                .dataSource(noCloseDataSource(connection))
                .locations("classpath:db/migration")
                .placeholders(mapOf("productSeedCount" to "10"))
                .load()
                .migrate()

            connection.createStatement().use { statement ->
                statement.executeUpdate("INSERT INTO carts (customer_id, status) VALUES (1, 'ACTIVE')")
                statement.executeUpdate(
                    """
                    INSERT INTO orders (
                        customer_id, cart_id, customer_name, customer_document, customer_document_type,
                        customer_email, shipping_street, shipping_number, shipping_neighborhood,
                        shipping_city, shipping_state, shipping_zip_code, shipping_country,
                        status, idempotency_key, request_fingerprint
                    ) VALUES (
                        1, 1, 'Ana Silva', '12345678900', 'CPF', 'ana@example.com',
                        'Rua A', '10', 'Centro', 'Sao Paulo', 'SP', '01000-000', 'BR',
                        'WAITING_PAYMENT', 'checkout-1', 'fingerprint-1'
                    )
                    """.trimIndent(),
                )
            }
            val orderId =
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT id FROM orders WHERE cart_id = 1").use { rows ->
                        rows.next()
                        rows.getLong(1)
                    }
                }

            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    INSERT INTO order_items (order_id, product_id, product_name, unit_price_amount, currency, quantity)
                    VALUES ($orderId, 10, 'Produto 10', 19.90, 'BRL', 2)
                    """.trimIndent(),
                )
            }

            assertEquals(1, count(connection, "orders"))
            assertEquals(1, count(connection, "order_items"))
            assertFailsWith<java.sql.SQLException> {
                connection.createStatement().use { statement ->
                    statement.executeUpdate("INSERT INTO carts (customer_id, status) VALUES (1, 'ACTIVE')")
                }
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        INSERT INTO orders (
                            customer_id, cart_id, customer_name, customer_document, customer_document_type,
                            customer_email, shipping_street, shipping_number, shipping_neighborhood,
                            shipping_city, shipping_state, shipping_zip_code, shipping_country,
                            status, idempotency_key, request_fingerprint
                        ) VALUES (
                            1, 2, 'Ana Silva', '12345678900', 'CPF', 'ana@example.com',
                            'Rua A', '10', 'Centro', 'Sao Paulo', 'SP', '01000-000', 'BR',
                            'WAITING_PAYMENT', 'checkout-1', 'fingerprint-2'
                        )
                        """.trimIndent(),
                    )
                }
            }.also { exception ->
                assertTrue(exception.message.orEmpty().contains("UQ_ORDERS_CUSTOMER_IDEMPOTENCY_KEY", ignoreCase = true))
            }
            assertFailsWith<java.sql.SQLException> {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        INSERT INTO orders (
                            customer_id, cart_id, customer_name, customer_document, customer_document_type,
                            customer_email, shipping_street, shipping_number, shipping_neighborhood,
                            shipping_city, shipping_state, shipping_zip_code, shipping_country,
                            status, idempotency_key, request_fingerprint
                        ) VALUES (
                            1, 1, 'Ana Silva', '12345678900', 'CPF', 'ana@example.com',
                            'Rua A', '10', 'Centro', 'Sao Paulo', 'SP', '01000-000', 'BR',
                            'WAITING_PAYMENT', 'checkout-2', 'fingerprint-2'
                        )
                        """.trimIndent(),
                    )
                }
            }
            assertTrue(
                count(
                    connection,
                    "INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'ORDERS' AND INDEX_NAME = 'IDX_ORDERS_CUSTOMER_CREATED_AT'",
                ) >=
                    1,
            )
        }
    }

    private fun count(
        connection: java.sql.Connection,
        fromClause: String,
    ): Int =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM $fromClause").use { rows ->
                rows.next()
                rows.getInt(1)
            }
        }

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
}
