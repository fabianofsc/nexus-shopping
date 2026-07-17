package com.nexus.shopping.customer

import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomerMigrationContractTest {
    @Test
    fun `customer seed should create ten customers with contacts and addresses`() {
        val jdbcUrl = "jdbc:h2:mem:customer_seed_contract;DB_CLOSE_DELAY=-1"
        Flyway
            .configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations("classpath:db/migration")
            .placeholders(mapOf("productSeedCount" to "10"))
            .load()
            .migrate()

        DriverManager.getConnection(jdbcUrl, "sa", "").use { connection ->
            assertEquals(10, countRows(connection, "customers"))
            assertEquals(10, countRows(connection, "customer_contacts"))
            assertEquals(10, countRows(connection, "customer_addresses"))
            assertEquals(0, countRows(connection, "customer_contacts cc LEFT JOIN customers c ON c.id = cc.customer_id WHERE c.id IS NULL"))
            assertEquals(0, countRows(connection, "customer_addresses ca LEFT JOIN customers c ON c.id = ca.customer_id WHERE c.id IS NULL"))
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
