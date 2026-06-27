package com.nexus.shopping.product

import java.sql.ResultSet
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ProductRepository(
    private val jdbcTemplate: JdbcTemplate,
) {

    fun findByCategoryId(categoryId: Long): List<Product> =
        jdbcTemplate.query("SELECT * FROM products WHERE category_id = ?", { resultSet, _ ->
            resultSet.toProduct()
        }, categoryId)

    fun findByName(name: String): List<Product> {
        val upperBound = nextLexicographicValue(name)

        return jdbcTemplate.query("SELECT * FROM products WHERE name >= ? AND name < ? AND name LIKE ?", { resultSet, _ ->
            resultSet.toProduct()
        }, name, upperBound, "$name%")
    }

    private fun nextLexicographicValue(value: String): String {
        val chars = value.toCharArray()
        for (index in chars.indices.reversed()) {
            if (chars[index] != Char.MAX_VALUE) {
                chars[index] = (chars[index].code + 1).toChar()
                return chars.concatToString(endIndex = index + 1)
            }
        }
        return value
    }

    private fun ResultSet.toProduct(): Product =
        Product(
            id = getLong("id"),
            brandId = getLong("brand_id"),
            categoryId = getLong("category_id"),
            sku = getString("sku"),
            name = getString("name"),
            slug = getString("slug"),
            description = getString("description"),
            status = getString("status"),
            priceAmount = getBigDecimal("price_amount"),
            currency = getString("currency"),
            inventoryQuantity = getInt("inventory_quantity"),
            createdAt = getTimestamp("created_at").toLocalDateTime(),
            updatedAt = getTimestamp("updated_at").toLocalDateTime(),
        )
}
