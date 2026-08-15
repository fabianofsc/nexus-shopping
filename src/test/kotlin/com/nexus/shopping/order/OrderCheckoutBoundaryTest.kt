package com.nexus.shopping.order

import com.nexus.shopping.integration.checkout.adapter.inbound.scheduler.PaymentReconciliationScheduler
import com.nexus.shopping.integration.checkout.adapter.outbound.acl.CartCheckoutGatewayAdapter
import com.nexus.shopping.integration.checkout.adapter.outbound.acl.NotificationGatewayAdapter
import com.nexus.shopping.integration.checkout.adapter.outbound.acl.OrderCreationGatewayAdapter
import com.nexus.shopping.integration.checkout.adapter.outbound.acl.OrderPaymentResultGatewayAdapter
import com.nexus.shopping.integration.checkout.adapter.outbound.acl.PaymentAuthorizationFingerprintGatewayAdapter
import com.nexus.shopping.integration.checkout.adapter.outbound.acl.PaymentProcessingGatewayAdapter
import com.nexus.shopping.integration.checkout.adapter.outbound.acl.PaymentReconciliationGatewayAdapter
import com.nexus.shopping.integration.checkout.adapter.outbound.acl.PaymentValidationGatewayAdapter
import com.nexus.shopping.integration.checkout.application.CheckoutWorkflowUseCase
import com.nexus.shopping.integration.checkout.application.PaymentReconciliationUseCase
import com.nexus.shopping.order.application.usecase.CreateOrderUseCase
import kotlin.test.Test
import kotlin.test.assertFalse

class OrderCheckoutBoundaryTest {
    @Test
    fun `Order creation boundary does not depend on Cart types`() {
        assertFalse(CreateOrderUseCase::class.java.boundaryTypes().any { it.name.startsWith("com.nexus.shopping.cart.") })
    }

    @Test
    fun `checkout application depends only on its own gateways`() {
        val forbiddenContexts =
            listOf(
                "com.nexus.shopping.cart.",
                "com.nexus.shopping.order.",
            )

        assertFalse(
            CheckoutWorkflowUseCase::class.java
                .boundaryTypes()
                .any { type -> forbiddenContexts.any(type.name::startsWith) },
        )
    }

    @Test
    fun `payment reconciliation orchestration depends only on its own gateways`() {
        val forbiddenContexts =
            listOf(
                "com.nexus.shopping.cart.",
                "com.nexus.shopping.order.",
                "com.nexus.shopping.payment.",
            )

        assertFalse(
            PaymentReconciliationUseCase::class.java
                .boundaryTypes()
                .any { type -> forbiddenContexts.any(type.name::startsWith) },
        )
    }

    @Test
    fun `payment reconciliation ACL and scheduler do not import payment or order domain types`() {
        val forbiddenDomainPackages =
            listOf(
                "com.nexus.shopping.payment.domain.",
                "com.nexus.shopping.order.domain.",
            )

        assertFalse(
            PaymentReconciliationGatewayAdapter::class.java
                .boundaryTypes()
                .any { type -> forbiddenDomainPackages.any(type.name::startsWith) },
        )
        assertFalse(
            PaymentReconciliationScheduler::class.java
                .boundaryTypes()
                .any { type -> forbiddenDomainPackages.any(type.name::startsWith) },
        )
    }

    @Test
    fun `checkout ACL adapters depend on inbound ports, not concrete use cases`() {
        val aclAdapters =
            listOf(
                CartCheckoutGatewayAdapter::class.java,
                NotificationGatewayAdapter::class.java,
                OrderCreationGatewayAdapter::class.java,
                OrderPaymentResultGatewayAdapter::class.java,
                PaymentAuthorizationFingerprintGatewayAdapter::class.java,
                PaymentProcessingGatewayAdapter::class.java,
                PaymentReconciliationGatewayAdapter::class.java,
                PaymentValidationGatewayAdapter::class.java,
            )

        aclAdapters.forEach { adapter ->
            val constructorParameterTypes = adapter.declaredConstructors.flatMap { it.parameterTypes.toList() }
            assertFalse(
                constructorParameterTypes.any { type -> type.name.contains(".application.usecase.") },
                "${adapter.name} should depend on an inbound port interface, not a concrete use case class",
            )
        }
    }
}

private fun Class<*>.boundaryTypes(): List<Class<*>> =
    declaredConstructors.flatMap { it.parameterTypes.toList() } +
        declaredFields.map { it.type } +
        declaredMethods.flatMap { it.parameterTypes.toList() + it.returnType }
