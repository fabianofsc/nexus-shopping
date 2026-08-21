package com.nexus.shopping.checkout

import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationSubmissionMigrationContractTest {
    @Test
    fun `migration cria journal sem interromper notifications legado`() {
        DriverManager.getConnection("jdbc:h2:mem:notification_submission_migration_contract;DB_CLOSE_DELAY=-1", "sa", "").use { connection ->
            Flyway
                .configure()
                .dataSource("jdbc:h2:mem:notification_submission_migration_contract;DB_CLOSE_DELAY=-1", "sa", "")
                .locations("classpath:db/migration")
                .placeholders(mapOf("productSeedCount" to "3"))
                .load()
                .migrate()

            assertEquals(1, countRows(connection, "INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'NOTIFICATIONS'"))
            assertEquals(1, countRows(connection, "INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'NOTIFICATION_SUBMISSIONS'"))
        }
    }

    private fun countRows(
        connection: Connection,
        fromClause: String,
    ): Int =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM $fromClause").use { resultSet ->
                resultSet.next()
                resultSet.getInt(1)
            }
        }
}
