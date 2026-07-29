package com.nexus.shopping.notification

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

class NotificationMigrationContractTest {
    @Test
    fun `notifications table has customer FK, check constraints, indexes and accepts a valid row`() {
        val jdbcUrl = "jdbc:h2:mem:notification_migration_contract;DB_CLOSE_DELAY=-1"

        // H2 2.4.240 evaluates CHECK constraints against the session that created them: a CHECK-constrained
        // insert issued from a new connection throws "Check constraint invalid" / "database has been closed",
        // even for fully valid data. Keeping Flyway's migration and this test's statements on the exact same
        // underlying connection avoids that engine limitation without weakening the migration itself
        // (the CHECK constraints work as expected against PostgreSQL in production).
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
                statement.executeUpdate(
                    """
                    INSERT INTO notifications
                        (customer_id, notification_key, recipient_email, type, channel, status, subject, body, reference_id)
                    VALUES
                        (1, 'order-confirmed:123:attempt-1', 'benjamin-duarte86@lexos.com.br',
                         'ORDER_CONFIRMED', 'EMAIL', 'PENDING',
                         'Pedido 123 confirmado', 'Seu pedido 123 no valor de 99.90 foi confirmado.', 123)
                    """.trimIndent(),
                )
            }
            assertEquals(1, countRows(connection, "notifications"))
            assertEquals(1, countRows(connection, "notifications WHERE customer_id = 1 AND status = 'PENDING'"))

            assertFailsWith<SQLException> {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        INSERT INTO notifications
                            (customer_id, notification_key, recipient_email, type, channel, status, subject, body)
                        VALUES
                            (1, 'order-confirmed:123:attempt-1', 'duplicate@example.com',
                             'ORDER_CONFIRMED', 'EMAIL', 'PENDING', 'x', 'y')
                        """.trimIndent(),
                    )
                }
            }

            assertFailsWith<SQLException> {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        INSERT INTO notifications
                            (customer_id, notification_key, recipient_email, type, channel, status, subject, body)
                        VALUES
                            (999999, 'ghost-key', 'ghost@example.com', 'ORDER_CONFIRMED', 'EMAIL', 'SENT', 'x', 'y')
                        """.trimIndent(),
                    )
                }
            }

            assertFailsWith<SQLException> {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        INSERT INTO notifications
                            (customer_id, notification_key, recipient_email, type, channel, status, subject, body)
                        VALUES
                            (1, 'invalid-key', 'invalid@example.com', 'NOT_A_REAL_TYPE', 'EMAIL', 'SENT', 'x', 'y')
                        """.trimIndent(),
                    )
                }
            }

            assertTrue(
                countRows(
                    connection,
                    "INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'NOTIFICATIONS' AND INDEX_NAME = 'IDX_NOTIFICATIONS_CUSTOMER_ID'",
                ) >= 1,
            )
            assertTrue(
                countRows(
                    connection,
                    "INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                        "WHERE TABLE_NAME = 'NOTIFICATIONS' AND CONSTRAINT_NAME = 'UQ_NOTIFICATIONS_NOTIFICATION_KEY' " +
                        "AND CONSTRAINT_TYPE = 'UNIQUE'",
                ) >= 1,
            )
            assertTrue(
                countRows(
                    connection,
                    "INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                        "WHERE TABLE_NAME = 'NOTIFICATIONS' AND CONSTRAINT_NAME = 'FK_NOTIFICATIONS_CUSTOMER'",
                ) >= 1,
            )
            for (checkConstraintName in listOf("NOTIFICATIONS_TYPE_CHECK", "NOTIFICATIONS_CHANNEL_CHECK", "NOTIFICATIONS_STATUS_CHECK")) {
                assertTrue(
                    countRows(
                        connection,
                        "INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                            "WHERE TABLE_NAME = 'NOTIFICATIONS' AND CONSTRAINT_NAME = '$checkConstraintName' " +
                            "AND CONSTRAINT_TYPE = 'CHECK'",
                    ) >= 1,
                    "expected CHECK constraint $checkConstraintName to exist",
                )
            }
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
