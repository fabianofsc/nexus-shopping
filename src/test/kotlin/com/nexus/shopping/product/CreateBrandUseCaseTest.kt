package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.command.CreateBrandCommand
import com.nexus.shopping.product.application.exception.BrandValidationException
import com.nexus.shopping.product.application.port.outbound.BrandRepositoryPort
import com.nexus.shopping.product.domain.Brand
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateBrandUseCaseTest {
    private var savedName: String? = null
    private val fakeRepo =
        object : BrandRepositoryPort {
            override fun findById(id: Long): Brand? = throw UnsupportedOperationException()

            override fun findAll(): List<Brand> = throw UnsupportedOperationException()

            override fun save(command: CreateBrandCommand): Brand {
                savedName = command.name
                return Brand(1L, command.name, command.description, LocalDateTime.now(), LocalDateTime.now())
            }
        }

    private val useCase = CreateBrandUseCase(fakeRepo)

    @Test
    fun `creates a brand`() {
        val brand = useCase.create(CreateBrandCommand("Apple", "Tech brand"))

        assertEquals("Apple", brand.name)
        assertEquals("Tech brand", brand.description)
        assertEquals("Apple", savedName)
    }

    @Test
    fun `throws BrandValidationException when name is blank`() {
        assertFailsWith<BrandValidationException> {
            useCase.create(CreateBrandCommand(" ", null))
        }
    }

    @Test
    fun `throws BrandValidationException when name exceeds the limit`() {
        assertFailsWith<BrandValidationException> {
            useCase.create(CreateBrandCommand("a".repeat(161), null))
        }
    }

    @Test
    fun `throws BrandValidationException when description exceeds the limit`() {
        assertFailsWith<BrandValidationException> {
            useCase.create(CreateBrandCommand("Apple", "a".repeat(1001)))
        }
    }
}
