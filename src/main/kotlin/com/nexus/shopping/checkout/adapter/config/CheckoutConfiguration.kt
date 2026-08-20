package com.nexus.shopping.checkout.adapter.config

import com.nexus.shopping.checkout.application.port.inbound.ExecuteCheckoutInputPort
import com.nexus.shopping.checkout.application.port.inbound.ReconcilePaymentsInputPort
import com.nexus.shopping.checkout.application.port.outbound.BillingGateway
import com.nexus.shopping.checkout.application.port.outbound.CheckoutCartGateway
import com.nexus.shopping.checkout.application.port.outbound.CheckoutCustomerGateway
import com.nexus.shopping.checkout.application.port.outbound.InventoryGateway
import com.nexus.shopping.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.checkout.application.port.outbound.OrderCreationGateway
import com.nexus.shopping.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentAuthorizationFingerprintGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentProcessingGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentReconciliationGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentValidationGateway
import com.nexus.shopping.checkout.application.port.outbound.ShippingGateway
import com.nexus.shopping.checkout.application.port.outbound.TransactionPort
import com.nexus.shopping.checkout.application.usecase.ExecuteCheckoutUseCase
import com.nexus.shopping.checkout.application.usecase.PaymentReconciliationUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CheckoutConfiguration {
    @Bean
    fun executeCheckoutUseCase(
        carts: CheckoutCartGateway,
        customers: CheckoutCustomerGateway,
        orders: OrderCreationGateway,
        paymentAuthorizationFingerprints: PaymentAuthorizationFingerprintGateway,
        paymentValidation: PaymentValidationGateway,
        payments: PaymentProcessingGateway,
        orderPaymentResults: OrderPaymentResultGateway,
        billing: BillingGateway,
        shipping: ShippingGateway,
        notifications: NotificationGateway,
        inventory: InventoryGateway,
        transaction: TransactionPort,
    ): ExecuteCheckoutInputPort =
        ExecuteCheckoutUseCase(
            carts = carts,
            customers = customers,
            orders = orders,
            paymentAuthorizationFingerprints = paymentAuthorizationFingerprints,
            paymentValidation = paymentValidation,
            payments = payments,
            orderPaymentResults = orderPaymentResults,
            billing = billing,
            shipping = shipping,
            notifications = notifications,
            inventory = inventory,
            transaction = transaction,
        )

    @Bean
    fun paymentReconciliationUseCase(
        reconciliation: PaymentReconciliationGateway,
        orderPaymentResults: OrderPaymentResultGateway,
        billing: BillingGateway,
        shipping: ShippingGateway,
        notifications: NotificationGateway,
        inventory: InventoryGateway,
    ): ReconcilePaymentsInputPort =
        PaymentReconciliationUseCase(
            reconciliation = reconciliation,
            orderPaymentResults = orderPaymentResults,
            billing = billing,
            shipping = shipping,
            notifications = notifications,
            inventory = inventory,
        )
}
