package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.usecase.CreateProductCommand
import com.nexus.shopping.product.domain.Currency
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SourceType
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(name = "products")
class ProductEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,

    @Column(name = "brand_id", nullable = false)
    var brandId: Long = 0,

    @Column(name = "category_id", nullable = false)
    var categoryId: Long = 0,

    @Column(name = "sku", nullable = false, length = 120)
    var sku: String = "",

    @Column(name = "name", nullable = false, length = 220)
    var name: String = "",

    @Column(name = "slug", nullable = false, length = 260)
    var slug: String = "",

    @Column(name = "description", length = 2000)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    var status: ProductStatus = ProductStatus.ACTIVE,

    @Column(name = "price_amount", nullable = false, precision = 12, scale = 2)
    var priceAmount: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    var currency: Currency = Currency.BRL,

    @Column(name = "inventory_quantity", nullable = false)
    var inventoryQuantity: Int = 0,

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null,
) {
    fun toDomain(): Product =
        Product(
            id = requireNotNull(id) { "ProductEntity.id must be available before mapping to domain." },
            brandId = brandId,
            categoryId = categoryId,
            sku = sku,
            name = name,
            slug = slug,
            description = description,
            status = status,
            priceAmount = priceAmount,
            currency = currency,
            inventoryQuantity = inventoryQuantity,
            createdAt = requireNotNull(createdAt) { "ProductEntity.createdAt must be available before mapping to domain." },
            updatedAt = requireNotNull(updatedAt) { "ProductEntity.updatedAt must be available before mapping to domain." },
        )
}

fun CreateProductCommand.toEntity(): ProductEntity =
    ProductEntity(
        brandId = brandId,
        categoryId = categoryId,
        sku = sku,
        name = name,
        slug = slug,
        description = description,
        status = ProductStatus.valueOf(status),
        priceAmount = priceAmount,
        currency = Currency.valueOf(currency),
        inventoryQuantity = inventoryQuantity,
    )
