package com.nexus.shopping.notification

import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NotificationMigrationContractTest {
    @Test
    fun `notifications table has customer FK, indexes and accepts a valid row`() {
        val jdbcUrl = "jdbc:h2:mem:notification_migration_contract;DB_CLOSE_DELAY=-1"
        Flyway
            .configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations("classpath:db/migration")
            .placeholders(mapOf("productSeedCount" to "10"))
            .load()
            .migrate()

        DriverManager.getConnection(jdbcUrl, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    INSERT INTO notifications
                        (customer_id, recipient_email, type, channel, status, subject, body, reference_id)
                    VALUES
                        (1, 'benjamin-duarte86@lexos.com.br', 'ORDER_CONFIRMED', 'EMAIL', 'SENT',
                         'Pedido 123 confirmado', 'Seu pedido 123 no valor de 99.90 foi confirmado.', 123)
                    """.trimIndent(),
                )
            }
            assertEquals(1, countRows(connection, "notifications"))
            assertEquals(1, countRows(connection, "notifications WHERE customer_id = 1 AND status = 'SENT'"))

            assertFailsWith<SQLException> {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        INSERT INTO notifications
                            (customer_id, recipient_email, type, channel, status, subject, body)
                        VALUES
                            (999999, 'ghost@example.com', 'ORDER_CONFIRMED', 'EMAIL', 'SENT', 'x', 'y')
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
                        "WHERE TABLE_NAME = 'NOTIFICATIONS' AND CONSTRAINT_NAME = 'FK_NOTIFICATIONS_CUSTOMER'",
                ) >= 1,
            )
        }
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
