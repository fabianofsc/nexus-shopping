package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.command.CreateCategoryCommand
import com.nexus.shopping.product.application.exception.CategoryValidationException
import com.nexus.shopping.product.domain.Category
import com.nexus.shopping.product.domain.CategoryStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SourceType
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "categories")
class CategoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "parent_id")
    var parentId: Long? = null,
    @Column(name = "name", nullable = false, length = 160)
    var name: String = "",
    @Column(name = "slug", nullable = false, length = 180)
    var slug: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    var status: CategoryStatus = CategoryStatus.ACTIVE,
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,
    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null,
) {
    fun toDomain(): Category = Category(id, parentId, name, slug, status, createdAt, updatedAt)
}

fun CreateCategoryCommand.toEntity(): CategoryEntity =
    CategoryEntity(
        parentId = parentId,
        name = name,
        slug = slug,
        status =
            try {
                CategoryStatus.valueOf(status)
            } catch (e: IllegalArgumentException) {
                throw CategoryValidationException("Invalid category status: $status")
            },
    )
