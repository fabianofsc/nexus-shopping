package com.nexus.shopping.customer

import com.nexus.shopping.customer.domain.CustomerStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomerStatusConstraintTest {
    @Test
    fun `CustomerStatus enum values match the CHECK constraint in V4 migration`() {
        val migration = java.nio.file.Path.of("src/main/resources/db/migration/V4__create_customer_context.sql").toFile().readText()
        val enumValues = CustomerStatus.entries.map { it.name }.toSet()
        val constraintValues =
            Regex("customers_status_check CHECK \\(status IN \\(([^)]*)\\)\\)")
                .find(migration)
                ?.groupValues
                ?.get(1)
                ?.split(",")
                ?.map { it.trim().removeSurrounding("'") }
                ?.toSet()

        assertEquals(enumValues, constraintValues)
    }
}
