package com.nexus.shopping.order.adapter.inbound.http.dto

import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.platform.adapter.inbound.http.dto.PageResponse
import com.nexus.shopping.platform.domain.PageResult
import java.math.BigDecimal
import java.time.Instant

data class OrderResponse(
    val id: Long,
    val customerId: Long,
    val cartId: Long,
    val customerSnapshot: OrderCustomerSnapshotResponse,
    val shippingAddressSnapshot: OrderShippingAddressSnapshotResponse,
    val items: List<OrderItemResponse>,
    val totalAmount: BigDecimal,
    val status: String,
    val createdAt: Instant,
    val cancelledAt: Instant?,
)

data class OrderCustomerSnapshotResponse(
    val customerId: Long,
    val name: String,
    val document: String,
    val documentType: String,
    val email: String,
    val phone: String?,
)

data class OrderShippingAddressSnapshotResponse(
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
)

data class OrderItemResponse(
    val productId: Long,
    val productName: String,
    val unitPriceAmount: BigDecimal,
    val currency: String,
    val quantity: Int,
    val totalAmount: BigDecimal,
)

fun Order.toResponse(): OrderResponse =
    OrderResponse(
        id = requireNotNull(id) { "Order.id must be available before mapping to response." },
        customerId = customerId,
        cartId = cartId,
        customerSnapshot =
            OrderCustomerSnapshotResponse(
                customerId = customerSnapshot.customerId,
                name = customerSnapshot.name,
                document = customerSnapshot.document,
                documentType = customerSnapshot.documentType,
                email = customerSnapshot.email,
                phone = customerSnapshot.phone,
            ),
        shippingAddressSnapshot =
            OrderShippingAddressSnapshotResponse(
                street = shippingAddressSnapshot.street,
                number = shippingAddressSnapshot.number,
                complement = shippingAddressSnapshot.complement,
                neighborhood = shippingAddressSnapshot.neighborhood,
                city = shippingAddressSnapshot.city,
                state = shippingAddressSnapshot.state,
                zipCode = shippingAddressSnapshot.zipCode,
                country = shippingAddressSnapshot.country,
            ),
        items = items.map { it.toResponse() },
        totalAmount = totalAmount,
        status = status.name,
        createdAt = requireNotNull(createdAt) { "Order.createdAt must be available before mapping to response." },
        cancelledAt = cancelledAt,
    )

fun OrderItemSnapshot.toResponse(): OrderItemResponse =
    OrderItemResponse(
        productId = productId,
        productName = productName,
        unitPriceAmount = unitPriceAmount,
        currency = currency.name,
        quantity = quantity,
        totalAmount = totalAmount,
    )

fun PageResult<Order>.toResponse(): PageResponse<OrderResponse> =
    PageResponse(
        content = content.map { it.toResponse() },
        page = page,
        size = size,
        count = count,
        hasNext = hasNext,
    )
