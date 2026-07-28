package com.nexus.shopping.integration.checkout.adapter.outbound.acl

import com.nexus.shopping.integration.checkout.application.model.CheckoutCustomerSnapshot
import com.nexus.shopping.integration.checkout.application.model.CheckoutItemSnapshot
import com.nexus.shopping.integration.checkout.application.model.CheckoutOrderSnapshot
import com.nexus.shopping.integration.checkout.application.model.CheckoutShippingAddressSnapshot
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
    override fun findReplay(command: FindCheckoutOrderReplayCommand): CheckoutOrderSnapshot? =
        replayOrders
            .findReplay(
                FindOrderReplayCommand(
                    customerId = command.customerId,
                    customerSnapshot = command.customerSnapshot.toOrderSnapshot(),
                    shippingAddressSnapshot = command.shippingAddressSnapshot.toOrderSnapshot(),
                    idempotencyKey = command.idempotencyKey,
                    paymentAuthorizationFingerprint = command.paymentAuthorizationFingerprint,
                ),
            )?.toCheckoutSnapshot()

    override fun create(command: CreateCheckoutOrderCommand): CheckoutOrderSnapshot =
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
            ).toCheckoutSnapshot()
}

private fun CheckoutCustomerSnapshot.toOrderSnapshot() = CustomerSnapshot(customerId, name, document, documentType, email, phone)

private fun CheckoutShippingAddressSnapshot.toOrderSnapshot() =
    ShippingAddressSnapshot(street, number, complement, neighborhood, city, state, zipCode, country)

private fun CheckoutItemSnapshot.toOrderSnapshot() =
    OrderItemSnapshot(productId, productName, unitPriceAmount, Currency.valueOf(currency), quantity)

private fun CreatedOrder.toCheckoutSnapshot(): CheckoutOrderSnapshot = order.toCheckoutSnapshot(replayed)

internal fun Order.toCheckoutSnapshot(replayed: Boolean): CheckoutOrderSnapshot {
    val orderId = requireNotNull(id)
    return CheckoutOrderSnapshot(
        id = orderId,
        orderReference = "checkout:$orderId",
        customerId = customerId,
        cartId = cartId,
        recipientEmail = customerSnapshot.email,
        customerSnapshot =
            CheckoutCustomerSnapshot(
                customerSnapshot.customerId,
                customerSnapshot.name,
                customerSnapshot.document,
                customerSnapshot.documentType,
                customerSnapshot.email,
                customerSnapshot.phone,
            ),
        shippingAddressSnapshot =
            CheckoutShippingAddressSnapshot(
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
                CheckoutItemSnapshot(
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
