package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.application.command.UpdatePriceCommand
import com.nexus.shopping.product.application.exception.ProductNotFoundException
import com.nexus.shopping.product.application.exception.ProductValidationException
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Currency
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import com.nexus.shopping.product.domain.ProductStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateProductPriceUseCaseTest {
    private fun aProduct(price: BigDecimal) =
        Product(
            id = 1L,
            brandId = 1L,
            categoryId = 1L,
            sku = "SKU-001",
            name = "Test Product",
            slug = "test-product",
            description = null,
            status = ProductStatus.ACTIVE,
            priceAmount = price,
            currency = Currency.BRL,
            inventoryQuantity = 0,
            createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        )

    private var repoReturn: Product? = null

    private val fakeRepo =
        object : ProductRepositoryPort {
            override fun findByCategoryId(
                categoryId: Long,
                page: Int,
                size: Int,
            ): ProductPage = throw UnsupportedOperationException()

            override fun findByName(
                name: String,
                page: Int,
                size: Int,
            ): ProductPage = throw UnsupportedOperationException()

            override fun save(command: CreateProductCommand): Product = throw UnsupportedOperationException()

            override fun updatePrice(
                id: Long,
                priceAmount: BigDecimal,
            ): Product? = repoReturn
        }

    private val useCase = UpdateProductPriceUseCase(fakeRepo)

    @Test
    fun `returns updated product when price is valid and product exists`() {
        repoReturn = aProduct(BigDecimal("99.90"))
        val result = useCase.execute(UpdatePriceCommand(1L, BigDecimal("99.90")))
        assertEquals(0, BigDecimal("99.90").compareTo(result.priceAmount))
    }

    @Test
    fun `throws ProductValidationException when priceAmount is zero`() {
        assertFailsWith<ProductValidationException> {
            useCase.execute(UpdatePriceCommand(1L, BigDecimal.ZERO))
        }
    }

    @Test
    fun `throws ProductValidationException when priceAmount is negative`() {
        assertFailsWith<ProductValidationException> {
            useCase.execute(UpdatePriceCommand(1L, BigDecimal("-1.00")))
        }
    }

    @Test
    fun `throws ProductNotFoundException when product does not exist`() {
        repoReturn = null
        assertFailsWith<ProductNotFoundException> {
            useCase.execute(UpdatePriceCommand(1L, BigDecimal("99.90")))
        }
    }
}
