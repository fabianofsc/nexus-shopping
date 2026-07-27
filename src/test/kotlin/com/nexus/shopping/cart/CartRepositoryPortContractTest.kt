package com.nexus.shopping.cart

import com.nexus.shopping.cart.application.port.outbound.CartRepositoryPort
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertTrue

class CartRepositoryPortContractTest {
    @Test
    fun `checkout lock and confirmation are mandatory repository capabilities`() {
        val methodsByName = CartRepositoryPort::class.java.declaredMethods.associateBy { it.name }

        assertTrue(Modifier.isAbstract(requireNotNull(methodsByName["reserveActiveCart"]).modifiers))
        assertTrue(Modifier.isAbstract(requireNotNull(methodsByName["confirmCheckout"]).modifiers))
    }
}
