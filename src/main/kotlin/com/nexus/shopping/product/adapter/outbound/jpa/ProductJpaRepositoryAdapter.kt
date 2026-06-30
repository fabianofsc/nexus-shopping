package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Repository
class ProductJpaRepositoryAdapter(
    private val repository: SpringDataProductRepository,
) : ProductRepositoryPort {
    @Transactional(readOnly = true)
    override fun findByCategoryId(
        categoryId: Long,
        page: Int,
        size: Int,
    ): ProductPage {
        val products =
            repository.findByCategoryId(
                categoryId = categoryId,
                pageable = PageRequest.of(page, size),
            )

        return products.toProductPage(page, size)
    }

    @Transactional(readOnly = true)
    override fun findByName(
        name: String,
        page: Int,
        size: Int,
    ): ProductPage {
        val upperBound = nextLexicographicValue(name)
        if (upperBound == name) return ProductPage(emptyList(), page, size, 0, false)
        val products =
            repository.findByNamePrefix(
                name = name,
                upperBound = upperBound,
                prefix = "$name%",
                pageable = PageRequest.of(page, size),
            )

        return products.toProductPage(page, size)
    }

    @Transactional
    override fun save(command: CreateProductCommand): Product = repository.saveAndFlush(command.toEntity()).toDomain()

    @Transactional
    override fun updatePrice(
        id: Long,
        priceAmount: BigDecimal,
    ): Product? {
        val updated = repository.updatePriceById(id, priceAmount)
        if (updated == 0) return null
        return repository.findById(id).orElse(null)?.toDomain()
    }

    private fun Slice<ProductEntity>.toProductPage(
        page: Int,
        size: Int,
    ): ProductPage {
        val productContent = content.map { it.toDomain() }

        return ProductPage(
            content = productContent,
            page = page,
            size = size,
            count = productContent.size,
            hasNext = hasNext(),
        )
    }

    private fun nextLexicographicValue(value: String): String {
        val chars = value.toCharArray()
        for (index in chars.indices.reversed()) {
            if (chars[index] != Char.MAX_VALUE) {
                chars[index] = (chars[index].code + 1).toChar()
                return chars.concatToString(endIndex = index + 1)
            }
        }
        return value
    }
}
