package com.nexus.shopping.product.adapter.outbound.jdbc

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.application.usecase.CreateProductCommand
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import java.math.BigDecimal
import java.sql.ResultSet
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class ProductRepository(
    private val jdbcTemplate: JdbcTemplate,
) : ProductRepositoryPort {

    override fun findByCategoryId(categoryId: Long, page: Int, size: Int): ProductPage {
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

    override fun findByName(name: String, page: Int, size: Int): ProductPage {
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

    override fun save(command: CreateProductCommand): Product {
        val id = SimpleJdbcInsert(jdbcTemplate)
            .withTableName("products")
            .usingGeneratedKeyColumns("id")
            .usingColumns(
                "brand_id", "category_id", "sku", "name", "slug", "description",
                "status", "price_amount", "currency", "inventory_quantity",
            )
            .executeAndReturnKey(
                mapOf(
                    "brand_id" to command.brandId,
                    "category_id" to command.categoryId,
                    "sku" to command.sku,
                    "name" to command.name,
                    "slug" to command.slug,
                    "description" to command.description,
                    "status" to command.status,
                    "price_amount" to command.priceAmount,
                    "currency" to command.currency,
                    "inventory_quantity" to command.inventoryQuantity,
                ),
            ).toLong()

        return jdbcTemplate.queryForObject("SELECT * FROM products WHERE id = ?", { rs, _ -> rs.toProduct() }, id)!!
    }

    @Transactional
    override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? {
        val updatedRows = jdbcTemplate.update(
            "UPDATE products SET price_amount = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            priceAmount,
            id,
        )

        if (updatedRows == 0) {
            return null
        }

        return jdbcTemplate.queryForObject("SELECT * FROM products WHERE id = ?", { rs, _ -> rs.toProduct() }, id)
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
