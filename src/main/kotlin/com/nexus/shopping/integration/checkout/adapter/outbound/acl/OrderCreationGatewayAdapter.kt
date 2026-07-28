package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.integration.checkout.application.model.CheckoutCustomerData
import com.nexus.shopping.integration.checkout.application.model.CheckoutItemData
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderData
import com.nexus.shopping.integration.checkout.application.model.CheckoutShippingAddressData
import com.nexus.shopping.integration.checkout.application.model.CreateCheckoutOrderCommand
import com.nexus.shopping.integration.checkout.application.model.FindCheckoutOrderReplayCommand
import com.nexus.shopping.integration.checkout.application.port.outbound.OrderCreationGateway
import com.nexus.shopping.order.application.command.CreateOrderCommand
import com.nexus.shopping.order.application.port.inbound.CreateOrderInputPort
import com.nexus.shopping.order.application.port.inbound.CreatedOrder
import com.nexus.shopping.order.application.port.inbound.FindOrderReplayCommand
import com.nexus.shopping.order.application.port.inbound.FindOrderReplayInputPort
import com.nexus.shopping.order.domain.Currency
import com.nexus.shopping.order.domain.CustomerSnapshot
import com.nexus.shopping.order.domain.Order
import com.nexus.shopping.order.domain.OrderItemSnapshot
import com.nexus.shopping.order.domain.ShippingAddressSnapshot
import org.springframework.stereotype.Component

@Component
class OrderCreationGatewayAdapter(
    private val orders: CreateOrderInputPort,
    private val replayOrders: FindOrderReplayInputPort,
) : OrderCreationGateway {
    override fun findReplay(command: FindCheckoutOrderReplayCommand): CheckoutOrderData? =
        replayOrders
            .findReplay(
                FindOrderReplayCommand(
                    customerId = command.customerId,
                    customerSnapshot = command.customerSnapshot.toOrderSnapshot(),
                    shippingAddressSnapshot = command.shippingAddressSnapshot.toOrderSnapshot(),
                    idempotencyKey = command.idempotencyKey,
                    paymentAuthorizationFingerprint = command.paymentAuthorizationFingerprint,
                ),
            )?.toCheckoutData()

    override fun create(command: CreateCheckoutOrderCommand): CheckoutOrderData =
        orders
            .create(
                CreateOrderCommand(
                    customerId = command.customerId,
                    cartId = command.cartId,
                    customerSnapshot = command.customerSnapshot.toOrderSnapshot(),
                    shippingAddressSnapshot = command.shippingAddressSnapshot.toOrderSnapshot(),
                    items = command.items.map { it.toOrderSnapshot() },
                    idempotencyKey = command.idempotencyKey,
                    paymentAuthorizationFingerprint = command.paymentAuthorizationFingerprint,
                ),
            ).toCheckoutData()
}

private fun CheckoutCustomerData.toOrderSnapshot() = CustomerSnapshot(customerId, name, document, documentType, email, phone)

private fun CheckoutShippingAddressData.toOrderSnapshot() =
    ShippingAddressSnapshot(street, number, complement, neighborhood, city, state, zipCode, country)

private fun CheckoutItemData.toOrderSnapshot() =
    OrderItemSnapshot(productId, productName, unitPriceAmount, Currency.valueOf(currency), quantity)

private fun CreatedOrder.toCheckoutData(): CheckoutOrderData = order.toCheckoutData(replayed)

internal fun Order.toCheckoutData(replayed: Boolean): CheckoutOrderData {
    val orderId = requireNotNull(id)
    return CheckoutOrderData(
        id = orderId,
        orderReference = "checkout:$orderId",
        customerId = customerId,
        cartId = cartId,
        recipientEmail = customerSnapshot.email,
        customerSnapshot =
            CheckoutCustomerData(
                customerSnapshot.customerId,
                customerSnapshot.name,
                customerSnapshot.document,
                customerSnapshot.documentType,
                customerSnapshot.email,
                customerSnapshot.phone,
            ),
        shippingAddressSnapshot =
            CheckoutShippingAddressData(
                shippingAddressSnapshot.street,
                shippingAddressSnapshot.number,
                shippingAddressSnapshot.complement,
                shippingAddressSnapshot.neighborhood,
                shippingAddressSnapshot.city,
                shippingAddressSnapshot.state,
                shippingAddressSnapshot.zipCode,
                shippingAddressSnapshot.country,
            ),
        items =
            items.map { item ->
                CheckoutItemData(
                    item.productId,
                    item.productName,
                    item.unitPriceAmount,
                    item.currency.name,
                    item.quantity,
                )
            },
        totalAmount = totalAmount,
        status = status.name,
        createdAt = requireNotNull(createdAt),
        cancelledAt = cancelledAt,
        replayed = replayed,
    )
}
