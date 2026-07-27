package com.nexus.shopping.integration.checkout.adapter.outbound.local

import com.nexus.shopping.integration.checkout.application.model.CheckoutCommand
import com.nexus.shopping.integration.checkout.application.model.CheckoutCustomerData
import com.nexus.shopping.integration.checkout.application.model.CheckoutItemData
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData
import com.nexus.shopping.integration.checkout.application.model.CheckoutShippingAddressData
import com.nexus.shopping.integration.checkout.application.model.CreateOrderData
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderCreationGateway
import com.nexus.shopping.order.application.command.CreateOrderCommand
import com.nexus.shopping.order.application.port.inbound.CreateOrderInputPort
import com.nexus.shopping.order.application.port.inbound.CreatedOrder
import com.nexus.shopping.order.application.port.inbound.FindOrderReplayCommand
import com.nexus.shopping.order.application.port.inbound.FindOrderReplayInputPort
import com.nexus.shopping.order.domain.Currency
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
import org.springframework.stereotype.Component

@Component
class LocalOrderCreationGateway(
    private val orders: CreateOrderInputPort,
    private val replayOrders: FindOrderReplayInputPort,
) : OrderCreationGateway {
    override fun findReplay(command: CheckoutCommand): CheckoutOrderData? =
        replayOrders
            .findReplay(
                FindOrderReplayCommand(
                    customerId = command.customerId,
                    customerSnapshot = command.customerSnapshot.toOrderSnapshot(),
                    shippingAddressSnapshot = command.shippingAddressSnapshot.toOrderSnapshot(),
                    idempotencyKey = command.idempotencyKey,
                ),
            )?.toCheckoutData()

    override fun create(data: CreateOrderData): CheckoutOrderData =
        orders
            .create(
                CreateOrderCommand(
                    customerId = data.customerId,
                    cartId = data.cartId,
                    customerSnapshot = data.customerSnapshot.toOrderSnapshot(),
                    shippingAddressSnapshot = data.shippingAddressSnapshot.toOrderSnapshot(),
                    items = data.items.map { it.toOrderSnapshot() },
                    idempotencyKey = data.idempotencyKey,
                ),
            ).toCheckoutData()
}

private fun CheckoutCustomerData.toOrderSnapshot() = CustomerSnapshot(customerId, name, document, documentType, email, phone)

private fun CheckoutShippingAddressData.toOrderSnapshot() =
    ShippingAddressSnapshot(street, number, complement, neighborhood, city, state, zipCode, country)

private fun CheckoutItemData.toOrderSnapshot() =
    OrderItemSnapshot(productId, productName, unitPriceAmount, Currency.valueOf(currency), quantity)

private fun CreatedOrder.toCheckoutData() =
    CheckoutOrderData(
        id = requireNotNull(order.id),
        customerId = order.customerId,
        cartId = order.cartId,
        customerSnapshot =
            CheckoutCustomerData(
                order.customerSnapshot.customerId,
                order.customerSnapshot.name,
                order.customerSnapshot.document,
                order.customerSnapshot.documentType,
                order.customerSnapshot.email,
                order.customerSnapshot.phone,
            ),
        shippingAddressSnapshot =
            CheckoutShippingAddressData(
                order.shippingAddressSnapshot.street,
                order.shippingAddressSnapshot.number,
                order.shippingAddressSnapshot.complement,
                order.shippingAddressSnapshot.neighborhood,
                order.shippingAddressSnapshot.city,
                order.shippingAddressSnapshot.state,
                order.shippingAddressSnapshot.zipCode,
                order.shippingAddressSnapshot.country,
            ),
        items =
            order.items.map { item ->
                CheckoutItemData(
                    item.productId,
                    item.productName,
                    item.unitPriceAmount,
                    item.currency.name,
                    item.quantity,
                )
            },
        totalAmount = order.totalAmount,
        status = order.status.name,
        createdAt = requireNotNull(order.createdAt),
        cancelledAt = order.cancelledAt,
        replayed = replayed,
    )
