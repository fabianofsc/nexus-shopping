package com.nexus.shopping.order

import com.nexus.shopping.integration.checkout.application.CheckoutWorkflowUseCase
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
}

private fun Class<*>.boundaryTypes(): List<Class<*>> =
    declaredConstructors.flatMap { it.parameterTypes.toList() } +
        declaredFields.map { it.type } +
        declaredMethods.flatMap { it.parameterTypes.toList() + it.returnType }
