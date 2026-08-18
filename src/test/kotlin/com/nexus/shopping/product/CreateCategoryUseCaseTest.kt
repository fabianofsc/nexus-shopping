package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.command.CreateCategoryCommand
import com.nexus.shopping.product.application.exception.CategoryValidationException
import com.nexus.shopping.product.application.port.outbound.CategoryRepositoryPort
import com.nexus.shopping.product.domain.Category
import com.nexus.shopping.product.domain.CategoryStatus
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateCategoryUseCaseTest {
    private var savedSlug: String? = null
    private val fakeRepo =
        object : CategoryRepositoryPort {
            override fun findById(id: Long): Category? = throw UnsupportedOperationException()

            override fun findAll(): List<Category> = throw UnsupportedOperationException()

            override fun save(command: CreateCategoryCommand): Category {
                savedSlug = command.slug
                return Category(
                    id = 1L,
                    parentId = command.parentId,
                    name = command.name,
                    slug = command.slug,
                    status = CategoryStatus.valueOf(command.status),
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )
            }

            override fun updateStatus(
                id: Long,
                status: CategoryStatus,
            ): Category? = throw UnsupportedOperationException()
        }

    private val useCase = CreateCategoryUseCase(fakeRepo)

    private fun validCommand() = CreateCategoryCommand(parentId = null, name = "Eletronicos", slug = "eletronicos", status = "ACTIVE")

    @Test
    fun `creates a category`() {
        val category = useCase.create(validCommand())

        assertEquals("Eletronicos", category.name)
        assertEquals("eletronicos", category.slug)
        assertEquals(CategoryStatus.ACTIVE, category.status)
        assertEquals("eletronicos", savedSlug)
    }

    @Test
    fun `throws CategoryValidationException when name is blank`() {
        assertFailsWith<CategoryValidationException> {
            useCase.create(validCommand().copy(name = " "))
        }
    }

    @Test
    fun `throws CategoryValidationException when slug is blank`() {
        assertFailsWith<CategoryValidationException> {
            useCase.create(validCommand().copy(slug = " "))
        }
    }

    @Test
    fun `throws CategoryValidationException when status is invalid`() {
        assertFailsWith<CategoryValidationException> {
            useCase.create(validCommand().copy(status = "DELETED"))
        }
    }

    @Test
    fun `throws CategoryValidationException when parentId is not positive`() {
        assertFailsWith<CategoryValidationException> {
            useCase.create(validCommand().copy(parentId = 0L))
        }
    }
}
