package com.nexus.shopping.checkout

import org.springframework.jdbc.core.JdbcTemplate

fun JdbcTemplate.seedStockedProduct(
    productId: Long = 10L,
    quantity: Int = 100,
) {
    val updated = update("UPDATE products SET inventory_quantity = ? WHERE id = ?", quantity, productId)
    if (updated == 0) {
        update(
            """
            INSERT INTO products (id, brand_id, category_id, sku, name, slug, status, price_amount, currency, inventory_quantity)
            VALUES (?, 1, 1, ?, ?, ?, 'ACTIVE', 19.90, 'BRL', ?)
            """.trimIndent(),
            productId,
            "SKU-$productId",
            "Product $productId",
            "product-$productId",
            quantity,
        )
    }
}

fun JdbcTemplate.stockOf(productId: Long): Int =
    requireNotNull(queryForObject("SELECT inventory_quantity FROM products WHERE id = ?", Int::class.java, productId))
