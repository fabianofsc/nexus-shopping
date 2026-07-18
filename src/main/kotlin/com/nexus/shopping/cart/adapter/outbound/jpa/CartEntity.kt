package com.nexus.shopping.cart.adapter.outbound.jpa

import com.nexus.shopping.cart.domain.Cart
import com.nexus.shopping.cart.domain.CartItem
import com.nexus.shopping.cart.domain.CartStatus
import com.nexus.shopping.cart.domain.Currency
import com.nexus.shopping.cart.domain.ProductSummary
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SourceType
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "carts")
class CartEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "customer_id", nullable = false)
    var customerId: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: CartStatus = CartStatus.ACTIVE,
    @OneToMany(mappedBy = "cart", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var items: MutableList<CartItemEntity> = mutableListOf(),
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {
    fun toDomain(): Cart =
        Cart(
            id = requireNotNull(id) { "CartEntity.id must be available before mapping to domain." },
            customerId = customerId,
            status = status,
            items = items.map { it.toDomain() },
            createdAt = requireNotNull(createdAt) { "CartEntity.createdAt must be available before mapping to domain." },
            updatedAt = requireNotNull(updatedAt) { "CartEntity.updatedAt must be available before mapping to domain." },
        )
}

@Entity
@Table(name = "cart_items")
class CartItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    var cart: CartEntity? = null,
    @Column(name = "product_id", nullable = false)
    var productId: Long = 0,
    @Column(name = "product_name", nullable = false, length = 220)
    var productName: String = "",
    @Column(name = "unit_price_amount", nullable = false, precision = 12, scale = 2)
    var unitPriceAmount: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    var currency: Currency = Currency.BRL,
    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0,
) {
    fun toDomain(): CartItem =
        CartItem(
            productSummary =
                ProductSummary(
                    productId = productId,
                    name = productName,
                    unitPriceAmount = unitPriceAmount,
                    currency = currency,
                ),
            quantity = quantity,
        )
}

/** Creates a brand new, not-yet-persisted [CartEntity] for [cart] (id must still be null). */
fun Cart.toNewEntity(): CartEntity =
    CartEntity(
        customerId = customerId,
        status = status,
    )
