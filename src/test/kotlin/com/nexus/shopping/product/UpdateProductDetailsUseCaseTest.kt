package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.platform.domain.PageResult
import com.nexus.shopping.product.application.command.CreateBrandCommand
import com.nexus.shopping.product.application.command.CreateCategoryCommand
import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.application.command.UpdateProductDetailsCommand
import com.nexus.shopping.product.application.exception.ProductNotFoundException
import com.nexus.shopping.product.application.exception.ProductValidationException
import com.nexus.shopping.product.application.port.outbound.BrandRepositoryPort
import com.nexus.shopping.product.application.port.outbound.CategoryRepositoryPort
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Brand
import com.nexus.shopping.product.domain.Category
import com.nexus.shopping.product.domain.CategoryStatus
import com.nexus.shopping.product.domain.Currency
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateProductDetailsUseCaseTest {
    private var updatedName: String? = null

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

            override fun archive(id: Long): Product? = throw UnsupportedOperationException()

            override fun updateDetails(
                id: Long,
                command: UpdateProductDetailsCommand,
            ): Product? {
                if (id != 1L) return null
                updatedName = command.name
                return aProduct().copy(
                    name = command.name,
                    slug = command.slug,
                    description = command.description,
                    brandId = command.brandId,
                    categoryId = command.categoryId,
                )
            }
        }

    private val brands =
        object : BrandRepositoryPort {
            override fun findById(id: Long): Brand? =
                if (id == 2L) Brand(2L, "Brand 2", null, LocalDateTime.now(), LocalDateTime.now()) else null

            override fun findAll(): List<Brand> = emptyList()

            override fun save(command: CreateBrandCommand): Brand = throw UnsupportedOperationException()
        }

    private val categories =
        object : CategoryRepositoryPort {
            override fun findById(id: Long): Category? =
                when (id) {
                    3L -> category(3L, CategoryStatus.ACTIVE)
                    4L -> category(4L, CategoryStatus.INACTIVE)
                    else -> null
                }

            override fun findAll(): List<Category> = emptyList()

            override fun save(command: CreateCategoryCommand): Category = throw UnsupportedOperationException()

            override fun updateStatus(
                id: Long,
                status: CategoryStatus,
            ): Category? = throw UnsupportedOperationException()
        }

    private val useCase = UpdateProductDetailsUseCase(fakeRepo, brands, categories)

    private fun category(
        id: Long,
        status: CategoryStatus,
    ) = Category(id, null, "Category $id", "category-$id", status, LocalDateTime.now(), LocalDateTime.now())

    @Test
    fun `updates product details`() {
        val product = useCase.execute(command())

        assertEquals("Updated Product", product.name)
        assertEquals("updated-product", product.slug)
        assertEquals("Updated Product", updatedName)
    }

    @Test
    fun `throws ProductNotFoundException when product does not exist`() {
        assertFailsWith<ProductNotFoundException> {
            useCase.execute(command(id = 999L))
        }
    }

    @Test
    fun `throws ProductValidationException when name is blank`() {
        assertFailsWith<ProductValidationException> {
            useCase.execute(command(name = " "))
        }
    }

    @Test
    fun `throws ProductValidationException when brandId or categoryId is not positive`() {
        assertFailsWith<ProductValidationException> {
            useCase.execute(command(brandId = 0L))
        }
        assertFailsWith<ProductValidationException> {
            useCase.execute(command(categoryId = 0L))
        }
    }

    @Test
    fun `throws ProductValidationException when brand or category does not exist`() {
        assertFailsWith<ProductValidationException> {
            useCase.execute(command(brandId = 999L))
        }
        assertFailsWith<ProductValidationException> {
            useCase.execute(command(categoryId = 999L))
        }
    }

    @Test
    fun `throws ProductValidationException when category is inactive`() {
        assertFailsWith<ProductValidationException> {
            useCase.execute(command(categoryId = 4L))
        }
    }

    private fun command(
        id: Long = 1L,
        name: String = "Updated Product",
        brandId: Long = 2L,
        categoryId: Long = 3L,
    ) = UpdateProductDetailsCommand(
        id = id,
        name = name,
        slug = "updated-product",
        description = null,
        brandId = brandId,
        categoryId = categoryId,
    )

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
}
