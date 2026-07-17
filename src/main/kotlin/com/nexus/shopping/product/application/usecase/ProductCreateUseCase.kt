package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.command.CreateProductCommand
import com.nexus.shopping.product.application.exception.ProductValidationException
import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Currency
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProductCreateUseCase(
    private val productRepository: ProductRepositoryPort,
) {
    fun create(command: CreateProductCommand): Product {
        logger.infoWithContext(
            "product.create.started",
            "product.brand_id" to command.brandId,
            "product.category_id" to command.categoryId,
        )

        if (command.brandId <= 0) throwValidationFailed("brandId must be greater than 0.")
        if (command.categoryId <= 0) throwValidationFailed("categoryId must be greater than 0.")
        if (command.sku.isBlank()) throwValidationFailed("sku must not be blank.")
        if (command.sku.length > 120) throwValidationFailed("sku must be at most 120 characters.")
        if (command.name.isBlank()) throwValidationFailed("name must not be blank.")
        if (command.name.length > 220) throwValidationFailed("name must be at most 220 characters.")
        if (command.slug.isBlank()) throwValidationFailed("slug must not be blank.")
        if (command.slug.length > 260) throwValidationFailed("slug must be at most 260 characters.")
        if (command.description != null && command.description.length > 2000) {
            throwValidationFailed("description must be at most 2000 characters.")
        }
        requireValidEnum<ProductStatus>(command.status, "status") { throwValidationFailed(it) }
        if (command.priceAmount < java.math.BigDecimal.ZERO) throwValidationFailed("priceAmount must be >= 0.")
        requireValidEnum<Currency>(command.currency, "currency") { throwValidationFailed(it) }
        if (command.inventoryQuantity < 0) throwValidationFailed("inventoryQuantity must be >= 0.")

        val product = productRepository.save(command)
        logger.infoWithContext(
            "product.create.completed",
            "product.id" to product.id,
            "product.brand_id" to product.brandId,
            "product.category_id" to product.categoryId,
        )
        return product
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("product.create.validation_failed", "validation.error" to message)
        throw ProductValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ProductCreateUseCase::class.java)
    }
}

private inline fun <reified T : Enum<T>> requireValidEnum(
    value: String,
    fieldName: String,
    exception: (String) -> Nothing,
) {
    val names = enumValues<T>().map { it.name }
    if (value !in names) exception("$fieldName must be one of: ${names.joinToString(", ")}.")
}
