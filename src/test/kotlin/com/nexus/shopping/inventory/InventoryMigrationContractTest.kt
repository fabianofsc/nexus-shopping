package com.nexus.shopping.inventory

import org.flywaydb.core.Flyway
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InventoryMigrationContractTest {
    private val migrationDirectory = Path.of("src/main/resources/db/migration")
    private val migration = migrationDirectory.resolve("V11__create_inventory_ledger.sql")

    @Test
    fun `inventory migration should keep portable relational SQL`() {
        val sql = Files.readString(migration).uppercase()

        assertFalse(
            Regex("\\bCREATE\\s+EXTENSION\\b").containsMatchIn(sql),
            "Keep the migration portable between PostgreSQL and H2.",
        )
        assertFalse(
            Regex("\\bUUID\\b|\\bTIMESTAMPTZ\\b|\\bGEN_RANDOM_UUID\\b").containsMatchIn(sql),
            "Keep the migration free from PostgreSQL-only column types and functions.",
        )
        assertFalse(
            Regex("\\bUNIQUE\\b").containsMatchIn(sql),
            "The ledger relies on the natural primary key; no implicit unique indexes.",
        )
    }

    @Test
    fun `inventory migration should create the stock ledger table with its indexes`() {
        val sql = Files.readString(migration)

        assertTrue(sql.contains("CREATE TABLE stock_movements"))
        assertTrue(sql.contains("CREATE INDEX idx_stock_movements_product_id ON stock_movements (product_id)"))
        assertTrue(sql.contains("CREATE INDEX idx_stock_movements_reference ON stock_movements (reference)"))
    }

    @Test
    fun `inventory migration should run on h2`() {
        val result =
            Flyway
                .configure()
                .dataSource("jdbc:h2:mem:inventory_migration_contract;DB_CLOSE_DELAY=-1", "sa", "")
                .locations("classpath:db/migration")
                .placeholders(mapOf("productSeedCount" to "10"))
                .load()
                .migrate()

        assertTrue(result.migrationsExecuted >= 11)
    }

    @Test
    fun `stock movements table should accept and expose ledger rows`() {
        val jdbcUrl = "jdbc:h2:mem:inventory_ledger_contract;DB_CLOSE_DELAY=-1"
        Flyway
            .configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations("classpath:db/migration")
            .placeholders(mapOf("productSeedCount" to "10"))
            .load()
            .migrate()

        DriverManager.getConnection(jdbcUrl, "sa", "").use { connection ->
            connection
                .prepareStatement(
                    "INSERT INTO stock_movements (product_id, order_id, quantity, movement_type, reference) " +
                        "VALUES (1, 10, 2, 'DECREASE', 'order-10')",
                ).use { it.executeUpdate() }

            connection
                .prepareStatement("SELECT quantity, movement_type, reference FROM stock_movements WHERE reference = 'order-10'")
                .use { statement ->
                    statement.executeQuery().use { rows ->
                        assertTrue(rows.next())
                        assertEquals(2, rows.getInt(1))
                        assertEquals("DECREASE", rows.getString(2))
                        assertEquals("order-10", rows.getString(3))
                    }
                }
        }
    }
}
