package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.platform.domain.PageResult
import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.application.exception.ProductNotFoundException
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Currency
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProductGetByIdUseCaseTest {
    private fun aProduct() =
        Product(
            id = 1L,
            brandId = 1L,
            categoryId = 1L,
            sku = "SKU-001",
            name = "Test Product",
            slug = "test-product",
            description = null,
            status = ProductStatus.ACTIVE,
            priceAmount = BigDecimal("19.90"),
            currency = Currency.BRL,
            inventoryQuantity = 0,
            createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        )

    private var repoReturn: Product? = null

    private val fakeRepo =
        object : ProductRepositoryPort {
            override fun findById(id: Long): Product? = repoReturn

            override fun findByCategoryId(
                categoryId: Long,
                page: Int,
                size: Int,
            ): PageResult<Product> = throw UnsupportedOperationException()

            override fun findByName(
                name: String,
                page: Int,
                size: Int,
            ): PageResult<Product> = throw UnsupportedOperationException()

            override fun save(command: CreateProductCommand): Product = throw UnsupportedOperationException()

            override fun updatePrice(
                id: Long,
                priceAmount: BigDecimal,
            ): Product? = throw UnsupportedOperationException()
        }

    private val useCase = ProductGetByIdUseCase(fakeRepo)

    @Test
    fun `returns product when product exists`() {
        repoReturn = aProduct()

        val result = useCase.execute(1L)

        assertEquals(1L, result.id)
    }

    @Test
    fun `throws ProductNotFoundException when product does not exist`() {
        repoReturn = null

        assertFailsWith<ProductNotFoundException> {
            useCase.execute(1L)
        }
    }
}
