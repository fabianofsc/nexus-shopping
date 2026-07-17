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
            assertEquals(3, countRows(connection, "customers WHERE document_type = 'CPF'"))
            assertEquals(2, countRows(connection, "customers WHERE document_type = 'CNH'"))
            assertEquals(3, countRows(connection, "customers WHERE document_type = 'RG'"))
            assertEquals(2, countRows(connection, "customers WHERE document_type = 'CNPJ'"))
            assertEquals(1, countRows(connection, "customers WHERE document = '02648629025' AND document_type = 'CPF'"))
            assertEquals(2, countRows(connection, "customers WHERE document = '58119974000' AND document_type = 'CPF'"))
            assertEquals(1, countRows(connection, "customers WHERE document = '18184222230' AND document_type = 'CNH'"))
            assertEquals(1, countRows(connection, "customers WHERE document = '71613090845' AND document_type = 'CNH'"))
            assertEquals(1, countRows(connection, "customers WHERE document = '378149714' AND document_type = 'RG'"))
            assertEquals(1, countRows(connection, "customers WHERE document = '108237126' AND document_type = 'RG'"))
            assertEquals(1, countRows(connection, "customers WHERE document = '265206510' AND document_type = 'RG'"))
            assertEquals(1, countRows(connection, "customers WHERE document = '34150598000117' AND document_type = 'CNPJ'"))
            assertEquals(1, countRows(connection, "customers WHERE document = '01879119000149' AND document_type = 'CNPJ'"))
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
