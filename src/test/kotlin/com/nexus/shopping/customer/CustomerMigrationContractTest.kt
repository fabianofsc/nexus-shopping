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
            assertEquals(10, countRows(connection, "(SELECT DISTINCT document FROM customers) distinct_documents"))
            assertEquals(
                1,
                countRows(
                    connection,
                    "customers WHERE name = 'Benjamin Bryan Duarte' AND document = '02648629025' AND document_type = 'CPF'",
                ),
            )
            assertEquals(
                1,
                countRows(
                    connection,
                    "customers WHERE name = 'Cláudia Elaine Eloá Galvão' AND document = '378149714' AND document_type = 'RG'",
                ),
            )
            assertEquals(
                1,
                countRows(
                    connection,
                    "customers WHERE name = 'Maitê Yasmin Cardoso' AND document = '01879119000149' AND document_type = 'CNPJ'",
                ),
            )
            assertEquals(1, countRows(connection, "customer_contacts WHERE email = 'benjamin-duarte86@lexos.com.br'"))
            assertEquals(1, countRows(connection, "customer_contacts WHERE email = 'maite.yasmin.cardoso@zoomfoccus.com.br'"))
            assertEquals(1, countRows(connection, "customer_addresses WHERE city = 'Brasília' AND zip_code = '71995275'"))
            assertEquals(1, countRows(connection, "customer_addresses WHERE city = 'Fortaleza' AND zip_code = '60416500'"))
            assertEquals(
                1,
                countRows(
                    connection,
                    "INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'CUSTOMER_CONTACTS' AND INDEX_NAME = 'IDX_CUSTOMER_CONTACTS_CUSTOMER_ID'",
                ),
            )
            assertEquals(
                1,
                countRows(
                    connection,
                    "INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'CUSTOMER_ADDRESSES' AND INDEX_NAME = 'IDX_CUSTOMER_ADDRESSES_CUSTOMER_ID'",
                ),
            )
            assertEquals(0, countRows(connection, "customer_contacts cc LEFT JOIN customers c ON c.id = cc.customer_id WHERE c.id IS NULL"))
            assertEquals(
                0,
                countRows(connection, "customer_addresses ca LEFT JOIN customers c ON c.id = ca.customer_id WHERE c.id IS NULL"),
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
