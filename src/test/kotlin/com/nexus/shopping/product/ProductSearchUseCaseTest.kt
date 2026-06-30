package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.application.exception.ProductValidationException
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProductSearchUseCaseTest {

    private var lastCalledMethod: String? = null

    private val fakeRepo = object : ProductRepositoryPort {
        override fun findByCategoryId(categoryId: Long, page: Int, size: Int): ProductPage {
            lastCalledMethod = "findByCategoryId"
            return ProductPage(content = emptyList(), page = page, size = size, count = 0, hasNext = false)
        }

        override fun findByName(name: String, page: Int, size: Int): ProductPage {
            lastCalledMethod = "findByName"
            return ProductPage(content = emptyList(), page = page, size = size, count = 0, hasNext = false)
        }

        override fun save(command: CreateProductCommand): Product = throw UnsupportedOperationException()

        override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? = throw UnsupportedOperationException()
    }

    private val useCase = ProductSearchUseCase(fakeRepo)

    @Test
    fun `search by categoryId delegates to findByCategoryId`() {
        val result = useCase.search(categoryId = 42L, name = null, page = 0, size = 10)
        assertEquals("findByCategoryId", lastCalledMethod)
        assertEquals(0, result.page)
        assertEquals(10, result.size)
    }

    @Test
    fun `search by name delegates to findByName`() {
        val result = useCase.search(categoryId = null, name = "notebook", page = 1, size = 20)
        assertEquals("findByName", lastCalledMethod)
        assertEquals(1, result.page)
        assertEquals(20, result.size)
    }

    @Test
    fun `search with both categoryId and name throws ProductValidationException`() {
        assertFailsWith<ProductValidationException> {
            useCase.search(categoryId = 1L, name = "notebook", page = 0, size = 10)
        }
    }

    @Test
    fun `search with neither categoryId nor name throws ProductValidationException`() {
        assertFailsWith<ProductValidationException> {
            useCase.search(categoryId = null, name = null, page = 0, size = 10)
        }
    }

    @Test
    fun `search with negative page throws ProductValidationException`() {
        assertFailsWith<ProductValidationException> {
            useCase.search(categoryId = 1L, name = null, page = -1, size = 10)
        }
    }

    @Test
    fun `search with size out of range throws ProductValidationException`() {
        assertFailsWith<ProductValidationException> {
            useCase.search(categoryId = 1L, name = null, page = 0, size = 501)
        }
    }
}
