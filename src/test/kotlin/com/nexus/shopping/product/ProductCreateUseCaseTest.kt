package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProductCreateUseCaseTest {

    private val fakeRepo = object : ProductRepositoryPort {
        override fun findByCategoryId(categoryId: Long, page: Int, size: Int) =
            ProductPage(content = emptyList(), page = page, size = size, count = 0, hasNext = false)

        override fun findByName(name: String, page: Int, size: Int) =
            ProductPage(content = emptyList(), page = page, size = size, count = 0, hasNext = false)

        override fun save(command: CreateProductCommand): Product = Product(
            id = 1L, brandId = command.brandId, categoryId = command.categoryId,
            sku = "SKU-TEST", name = command.name, slug = command.slug,
            description = command.description, status = command.status,
            priceAmount = command.priceAmount, currency = command.currency,
            inventoryQuantity = command.inventoryQuantity,
            createdAt = java.time.LocalDateTime.now(), updatedAt = java.time.LocalDateTime.now(),
        )
    }

    private val useCase = ProductCreateUseCase(fakeRepo)

    private fun validCommand() = CreateProductCommand(
        brandId = 1L,
        categoryId = 1L,
        sku = "SKU-001",
        name = "Product Name",
        slug = "product-name",
        priceAmount = BigDecimal("29.90"),
    )

    @Test
    fun `create valid product delegates to repository`() {
        val result = useCase.create(validCommand())
        assertEquals("SKU-TEST", result.sku)
    }

    @Test
    fun `create with blank sku throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(sku = "  "))
        }
    }

    @Test
    fun `create with sku over 120 chars throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(sku = "a".repeat(121)))
        }
    }

    @Test
    fun `create with blank name throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(name = ""))
        }
    }

    @Test
    fun `create with name over 220 chars throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(name = "a".repeat(221)))
        }
    }

    @Test
    fun `create with blank slug throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(slug = ""))
        }
    }

    @Test
    fun `create with description over 2000 chars throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(description = "a".repeat(2001)))
        }
    }

    @Test
    fun `create with invalid status throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(status = "DELETED"))
        }
    }

    @Test
    fun `create with negative priceAmount throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(priceAmount = BigDecimal("-0.01")))
        }
    }

    @Test
    fun `create with currency length not 3 throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(currency = "US"))
        }
    }

    @Test
    fun `create with negative inventoryQuantity throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(inventoryQuantity = -1))
        }
    }

    @Test
    fun `create with zero brandId throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(brandId = 0L))
        }
    }

    @Test
    fun `create with zero categoryId throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(categoryId = 0L))
        }
    }
}
