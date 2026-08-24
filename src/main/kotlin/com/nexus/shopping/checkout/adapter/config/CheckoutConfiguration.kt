package com.nexus.shopping.checkout.adapter.config

import com.nexus.shopping.checkout.application.port.inbound.ExecuteCheckoutInputPort
import com.nexus.shopping.checkout.application.port.inbound.NotificationSubmissionBackofficeInputPort
import com.nexus.shopping.checkout.application.port.inbound.ReconcilePaymentsInputPort
import com.nexus.shopping.checkout.application.port.outbound.BillingGateway
import com.nexus.shopping.checkout.application.port.outbound.CheckoutCartGateway
import com.nexus.shopping.checkout.application.port.outbound.CheckoutCustomerGateway
import com.nexus.shopping.checkout.application.port.outbound.InventoryGateway
import com.nexus.shopping.checkout.application.port.outbound.NotificationGateway
import com.nexus.shopping.checkout.application.port.outbound.NotificationServiceClientPort
import com.nexus.shopping.checkout.application.port.outbound.NotificationSubmissionRepositoryPort
import com.nexus.shopping.checkout.application.port.outbound.OrderCreationGateway
import com.nexus.shopping.checkout.application.port.outbound.OrderPaymentResultGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentAuthorizationFingerprintGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentProcessingGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentReconciliationGateway
import com.nexus.shopping.checkout.application.port.outbound.PaymentValidationGateway
import com.nexus.shopping.checkout.application.port.outbound.ShippingGateway
import com.nexus.shopping.checkout.application.port.outbound.TransactionPort
import com.nexus.shopping.checkout.application.usecase.ExecuteCheckoutUseCase
import com.nexus.shopping.checkout.application.usecase.NotificationSubmissionUseCase
import com.nexus.shopping.checkout.application.usecase.PaymentReconciliationUseCase
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class CheckoutConfiguration {
    @Bean
    @ConditionalOnBean(NotificationServiceClientPort::class)
    fun notificationSubmissionUseCase(
        repository: NotificationSubmissionRepositoryPort,
        client: NotificationServiceClientPort,
    ): NotificationSubmissionUseCase = NotificationSubmissionUseCase(repository, client)

    @Bean
    @Primary
    @ConditionalOnBean(NotificationSubmissionUseCase::class)
    fun notificationGateway(notificationSubmissions: NotificationSubmissionUseCase): NotificationGateway = notificationSubmissions

    @Bean
    @ConditionalOnBean(NotificationSubmissionUseCase::class)
    fun notificationSubmissionBackofficeInputPort(
        notificationSubmissions: NotificationSubmissionUseCase,
    ): NotificationSubmissionBackofficeInputPort = notificationSubmissions

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
        transaction: TransactionPort,
    ): ReconcilePaymentsInputPort =
        PaymentReconciliationUseCase(
            reconciliation = reconciliation,
            orderPaymentResults = orderPaymentResults,
            billing = billing,
            shipping = shipping,
            notifications = notifications,
            inventory = inventory,
            transaction = transaction,
        )
}
