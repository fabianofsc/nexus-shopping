package com.nexus.shopping.product

import com.nexus.shopping.product.domain.ProductStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProductStatusConstraintTest {
    @Test
    fun `ProductStatus enum values match the CHECK constraint in V1 migration`() {
        val constraintValues = setOf("ACTIVE", "INACTIVE", "ARCHIVED")
        val enumValues = ProductStatus.entries.map { it.name }.toSet()
        assertEquals(
            constraintValues,
            enumValues,
            "Update products_status_check in V1__create_product_catalog.sql when adding/removing ProductStatus values",
        )
    }
}
