package com.nexus.shopping.payment

import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaymentMigrationContractTest {
    @Test
    fun `payment attempts persist authorization fingerprint and enforce independent identities`() {
        DriverManager.getConnection("jdbc:h2:mem:payment_migration_contract;DB_CLOSE_DELAY=-1", "sa", "").use { connection ->
            Flyway
                .configure()
                .dataSource("jdbc:h2:mem:payment_migration_contract;DB_CLOSE_DELAY=-1", "sa", "")
                .locations("classpath:db/migration")
                .placeholders(mapOf("productSeedCount" to "10"))
                .load()
                .migrate()

            insertAttempt(connection, "pay-1", "order-1", "idem-1", "fingerprint-1")

            assertEquals("fingerprint-1", textColumn(connection, "authorization_fingerprint", "pay-1"))
            assertFailsWith<java.sql.SQLException> {
                insertAttempt(connection, "pay-2", "order-1", "idem-1", "fingerprint-2")
            }
            assertFailsWith<java.sql.SQLException> {
                insertAttempt(connection, "pay-1", "order-2", "idem-2", "fingerprint-3")
            }
            assertEquals(0, foreignKeyCount(connection))
        }
    }

    private fun insertAttempt(
        connection: Connection,
        attemptReference: String,
        referenceId: String,
        idempotencyKey: String,
        fingerprint: String,
    ) {
        connection
            .prepareStatement(
                """
                INSERT INTO payment_attempts (
                    attempt_reference, reference_id, amount, currency, status, provider,
                    idempotency_key, authorization_fingerprint, processing_lease_token, processing_lease_until
                ) VALUES (?, ?, 19.90, 'BRL', 'REQUESTED', 'LOGGING_PROVIDER', ?, ?, ?, CURRENT_TIMESTAMP)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, attemptReference)
                statement.setString(2, referenceId)
                statement.setString(3, idempotencyKey)
                statement.setString(4, fingerprint)
                statement.setString(5, UUID.randomUUID().toString())
                statement.executeUpdate()
            }
    }

    private fun textColumn(
        connection: Connection,
        column: String,
        attemptReference: String,
    ): String =
        connection.prepareStatement("SELECT $column FROM payment_attempts WHERE attempt_reference = ?").use { statement ->
            statement.setString(1, attemptReference)
            statement.executeQuery().use { rows ->
                rows.next()
                rows.getString(1)
            }
        }

    private fun foreignKeyCount(connection: Connection): Int =
        connection.createStatement().use { statement ->
            statement
                .executeQuery(
                    """
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                    WHERE TABLE_NAME = 'PAYMENT_ATTEMPTS' AND CONSTRAINT_TYPE = 'FOREIGN KEY'
                    """.trimIndent(),
                ).use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
        }
}
