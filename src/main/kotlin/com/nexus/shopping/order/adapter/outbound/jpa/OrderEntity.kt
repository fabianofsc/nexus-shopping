package com.nexus.shopping.order.adapter.outbound.jpa

import com.nexus.shopping.order.domain.Currency
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.OrderStatus
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
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
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SourceType
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "orders")
class OrderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "customer_id", nullable = false)
    var customerId: Long = 0,
    @Column(name = "cart_id", nullable = false)
    var cartId: Long = 0,
    @Column(name = "customer_name", nullable = false, length = 220)
    var customerName: String = "",
    @Column(name = "customer_document", nullable = false, length = 64)
    var customerDocument: String = "",
    @Column(name = "customer_document_type", nullable = false, length = 32)
    var customerDocumentType: String = "",
    @Column(name = "customer_email", nullable = false, length = 320)
    var customerEmail: String = "",
    @Column(name = "customer_phone", length = 64)
    var customerPhone: String? = null,
    @Column(name = "shipping_street", nullable = false, length = 220)
    var shippingStreet: String = "",
    @Column(name = "shipping_number", nullable = false, length = 32)
    var shippingNumber: String = "",
    @Column(name = "shipping_complement", length = 220)
    var shippingComplement: String? = null,
    @Column(name = "shipping_neighborhood", nullable = false, length = 220)
    var shippingNeighborhood: String = "",
    @Column(name = "shipping_city", nullable = false, length = 160)
    var shippingCity: String = "",
    @Column(name = "shipping_state", nullable = false, length = 80)
    var shippingState: String = "",
    @Column(name = "shipping_zip_code", nullable = false, length = 32)
    var shippingZipCode: String = "",
    @Column(name = "shipping_country", nullable = false, length = 80)
    var shippingCountry: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: OrderStatus = OrderStatus.WAITING_PAYMENT,
    @Column(name = "idempotency_key", nullable = false, length = 255)
    var idempotencyKey: String = "",
    @Column(name = "request_fingerprint", nullable = false, length = 64)
    var requestFingerprint: String = "",
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<OrderItemEntity> = mutableListOf(),
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null,
) {
    fun toDomain(): Order =
        Order(
            id = requireNotNull(id) { "OrderEntity.id must be available before mapping to domain." },
            customerId = customerId,
            cartId = cartId,
            customerSnapshot =
                CustomerSnapshot(
                    customerId,
                    customerName,
                    customerDocument,
                    customerDocumentType,
                    customerEmail,
                    customerPhone,
                ),
            shippingAddressSnapshot =
                ShippingAddressSnapshot(
                    shippingStreet,
                    shippingNumber,
                    shippingComplement,
                    shippingNeighborhood,
                    shippingCity,
                    shippingState,
                    shippingZipCode,
                    shippingCountry,
                ),
            items = items.map { it.toDomain() },
            status = status,
            idempotencyKey = idempotencyKey,
            requestFingerprint = requestFingerprint,
            createdAt = requireNotNull(createdAt) { "OrderEntity.createdAt must be available before mapping to domain." },
            cancelledAt = cancelledAt,
        )
}

@Entity
@Table(name = "order_items")
class OrderItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: OrderEntity? = null,
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
    fun toDomain(): OrderItemSnapshot = OrderItemSnapshot(productId, productName, unitPriceAmount, currency, quantity)
}

fun Order.toEntity(): OrderEntity =
    OrderEntity(
        id = id,
        customerId = customerId,
        cartId = cartId,
        customerName = customerSnapshot.name,
        customerDocument = customerSnapshot.document,
        customerDocumentType = customerSnapshot.documentType,
        customerEmail = customerSnapshot.email,
        customerPhone = customerSnapshot.phone,
        shippingStreet = shippingAddressSnapshot.street,
        shippingNumber = shippingAddressSnapshot.number,
        shippingComplement = shippingAddressSnapshot.complement,
        shippingNeighborhood = shippingAddressSnapshot.neighborhood,
        shippingCity = shippingAddressSnapshot.city,
        shippingState = shippingAddressSnapshot.state,
        shippingZipCode = shippingAddressSnapshot.zipCode,
        shippingCountry = shippingAddressSnapshot.country,
        status = status,
        idempotencyKey = idempotencyKey,
        requestFingerprint = requestFingerprint,
        cancelledAt = cancelledAt,
    ).also { entity ->
        entity.items =
            items
                .map { item ->
                    OrderItemEntity(
                        order = entity,
                        productId = item.productId,
                        productName = item.productName,
                        unitPriceAmount = item.unitPriceAmount,
                        currency = item.currency,
                        quantity = item.quantity,
                    )
                }.toMutableList()
    }
