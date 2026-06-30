package com.nexus.shopping

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogMigrationContractTest {
    private val migrationDirectory = Path.of("src/main/resources/db/migration")
    private val schemaMigration = migrationDirectory.resolve("V1__create_product_catalog.sql")
    private val applicationConfig = Path.of("src/main/resources/application.yml")

    @Test
    fun `catalog migrations should keep portable relational SQL`() {
        val sql = readAllMigrations()
        val normalizedSql = sql.uppercase()

        assertFalse(
            Regex("\\bUNIQUE\\b").containsMatchIn(normalizedSql),
            "Do not add constraints that create implicit indexes outside the explicit index example.",
        )
        assertFalse(
            Regex("\\bCREATE\\s+EXTENSION\\b").containsMatchIn(normalizedSql),
            "Keep the migration portable between PostgreSQL and H2.",
        )
        assertFalse(
            Regex("\\bUUID\\b|\\bTIMESTAMPTZ\\b|\\bGEN_RANDOM_UUID\\b").containsMatchIn(normalizedSql),
            "Keep the migration free from PostgreSQL-only column types and functions.",
        )
    }

    @Test
    fun `catalog index migration should include targeted read indexes`() {
        val indexMigration = Files.readString(migrationDirectory.resolve("V3__add_product_read_indexes.sql"))
        val normalizedSql = indexMigration.uppercase()

        assertTrue(normalizedSql.contains("CREATE INDEX IDX_PRODUCTS_CATEGORY_ID ON PRODUCTS (CATEGORY_ID)"))
        assertTrue(normalizedSql.contains("CREATE INDEX IDX_PRODUCTS_NAME ON PRODUCTS (NAME)"))
        assertFalse(
            Regex("\\bUNIQUE\\b").containsMatchIn(normalizedSql),
            "The performance example should use non-unique read indexes.",
        )
    }

    @Test
    fun `catalog migration should include the core product catalog tables`() {
        val sql = Files.readString(schemaMigration)

        val tableNames = listOf("brands", "categories", "products")

        tableNames.forEach { tableName ->
            assertTrue(
                sql.contains("CREATE TABLE $tableName"),
                "Expected catalog migration to create $tableName.",
            )
        }

        assertFalse(
            Regex("\\bCREATE\\s+TABLE\\b").findAll(sql.uppercase()).count() > tableNames.size,
            "Keep the catalog schema intentionally small for the performance demonstration.",
        )
    }

    @Test
    fun `catalog migrations should run on h2`() {
        val result =
            Flyway
                .configure()
                .dataSource("jdbc:h2:mem:catalog_migration_contract;DB_CLOSE_DELAY=-1", "sa", "")
                .locations("classpath:db/migration")
                .placeholders(mapOf("productSeedCount" to "10"))
                .load()
                .migrate()

        assertTrue(result.migrationsExecuted >= 3)
    }

    @Test
    fun `catalog seed should target ten million products by default`() {
        val applicationYaml = Files.readString(applicationConfig)
        val seedMigration = Files.readString(migrationDirectory.resolve("V2__seed_product_catalog.sql"))

        assertTrue(applicationYaml.contains("PRODUCT_SEED_COUNT:10000000"))
        assertTrue(seedMigration.contains("\${productSeedCount}"))
    }

    @Test
    fun `catalog seed should create relationally valid products`() {
        val jdbcUrl = "jdbc:h2:mem:catalog_seed_contract;DB_CLOSE_DELAY=-1"
        Flyway
            .configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations("classpath:db/migration")
            .placeholders(mapOf("productSeedCount" to "100"))
            .load()
            .migrate()

        DriverManager.getConnection(jdbcUrl, "sa", "").use { connection ->
            assertEquals(1000, countRows(connection, "brands"))
            assertEquals(500, countRows(connection, "categories"))
            assertEquals(100, countRows(connection, "products"))
            assertEquals(0, countRows(connection, "products p LEFT JOIN brands b ON b.id = p.brand_id WHERE b.id IS NULL"))
            assertEquals(0, countRows(connection, "products p LEFT JOIN categories c ON c.id = p.category_id WHERE c.id IS NULL"))
        }
    }

    private fun readAllMigrations(): String =
        Files.list(migrationDirectory).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .sorted()
                .map { Files.readString(it) }
                .toList()
                .joinToString(separator = "\n")
        }

    private fun countRows(
        connection: java.sql.Connection,
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
