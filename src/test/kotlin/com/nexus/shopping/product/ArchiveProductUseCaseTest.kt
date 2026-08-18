package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.platform.domain.PageResult
import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.application.command.UpdateProductDetailsCommand
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

class ArchiveProductUseCaseTest {
    private fun aProduct(status: ProductStatus) =
        Product(
            id = 1L,
            brandId = 1L,
            categoryId = 1L,
            sku = "SKU-001",
            name = "Test Product",
            slug = "test-product",
            description = null,
            status = status,
            priceAmount = BigDecimal("19.90"),
            currency = Currency.BRL,
            inventoryQuantity = 0,
            createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        )

    private var archivedStatus: ProductStatus? = null

    private val fakeRepo =
        object : ProductRepositoryPort {
            override fun findById(id: Long): Product? = throw UnsupportedOperationException()

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

            override fun archive(id: Long): Product? {
                if (id != 1L) return null
                archivedStatus = ProductStatus.ARCHIVED
                return aProduct(ProductStatus.ARCHIVED)
            }

            override fun updateDetails(
                id: Long,
                command: UpdateProductDetailsCommand,
            ): Product? = throw UnsupportedOperationException()
        }

    private val useCase = ArchiveProductUseCase(fakeRepo)

    @Test
    fun `archives an active product`() {
        val product = useCase.execute(1L)

        assertEquals(ProductStatus.ARCHIVED, product.status)
        assertEquals(ProductStatus.ARCHIVED, archivedStatus)
    }

    @Test
    fun `throws ProductNotFoundException when product does not exist`() {
        assertFailsWith<ProductNotFoundException> {
            useCase.execute(999L)
        }
    }
}
