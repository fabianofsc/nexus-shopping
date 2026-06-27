package com.nexus.shopping.product

import java.sql.ResultSet
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ProductRepository(
    private val jdbcTemplate: JdbcTemplate,
) {

    fun findByCategoryId(categoryId: Long, page: Int, size: Int): ProductPage {
        val products = jdbcTemplate.query(
            "SELECT * FROM products WHERE category_id = ? ORDER BY id LIMIT ? OFFSET ?",
            { resultSet, _ ->
                resultSet.toProduct()
            },
            categoryId,
            size + 1,
            page.toOffset(size),
        )

        return products.toPage(page, size)
    }

    fun findByName(name: String, page: Int, size: Int): ProductPage {
        val upperBound = nextLexicographicValue(name)

        val products = jdbcTemplate.query(
            "SELECT * FROM products WHERE name >= ? AND name < ? AND name LIKE ? ORDER BY name LIMIT ? OFFSET ?",
            { resultSet, _ ->
                resultSet.toProduct()
            },
            name,
            upperBound,
            "$name%",
            size + 1,
            page.toOffset(size),
        )

        return products.toPage(page, size)
    }

    private fun List<Product>.toPage(page: Int, size: Int): ProductPage {
        val hasNext = this.size > size
        val content = if (hasNext) take(size) else this

        return ProductPage(
            content = content,
            page = page,
            size = size,
            count = content.size,
            hasNext = hasNext,
        )
    }

    private fun Int.toOffset(size: Int): Long = this.toLong() * size.toLong()

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
