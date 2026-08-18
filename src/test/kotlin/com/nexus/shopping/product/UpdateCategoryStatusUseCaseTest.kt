package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.command.CreateCategoryCommand
import com.nexus.shopping.product.application.command.UpdateCategoryStatusCommand
import com.nexus.shopping.product.application.exception.CategoryNotFoundException
import com.nexus.shopping.product.application.exception.CategoryValidationException
import com.nexus.shopping.product.application.port.outbound.CategoryRepositoryPort
import com.nexus.shopping.product.domain.Category
import com.nexus.shopping.product.domain.CategoryStatus
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateCategoryStatusUseCaseTest {
    private var updatedId: Long? = null
    private var updatedStatus: CategoryStatus? = null
    private val fakeRepo =
        object : CategoryRepositoryPort {
            override fun findById(id: Long): Category? = throw UnsupportedOperationException()

            override fun findAll(): List<Category> = throw UnsupportedOperationException()

            override fun save(command: CreateCategoryCommand): Category = throw UnsupportedOperationException()

            override fun updateStatus(
                id: Long,
                status: CategoryStatus,
            ): Category? {
                if (id != 1L) return null
                updatedId = id
                updatedStatus = status
                return Category(1L, null, "Eletronicos", "eletronicos", status, LocalDateTime.now(), LocalDateTime.now())
            }
        }

    private val useCase = UpdateCategoryStatusUseCase(fakeRepo)

    @Test
    fun `updates the category status`() {
        val category = useCase.execute(UpdateCategoryStatusCommand(1L, "INACTIVE"))

        assertEquals(CategoryStatus.INACTIVE, category.status)
        assertEquals(1L, updatedId)
        assertEquals(CategoryStatus.INACTIVE, updatedStatus)
    }

    @Test
    fun `throws CategoryNotFoundException when category does not exist`() {
        assertFailsWith<CategoryNotFoundException> {
            useCase.execute(UpdateCategoryStatusCommand(999L, "ACTIVE"))
        }
    }

    @Test
    fun `throws CategoryValidationException when status is invalid`() {
        assertFailsWith<CategoryValidationException> {
            useCase.execute(UpdateCategoryStatusCommand(1L, "DELETED"))
        }
    }
}
