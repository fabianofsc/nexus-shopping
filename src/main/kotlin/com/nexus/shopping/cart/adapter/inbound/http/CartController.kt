package com.nexus.shopping.cart.adapter.inbound.http

import com.nexus.shopping.cart.adapter.inbound.http.dto.AddCartItemRequest
import com.nexus.shopping.cart.adapter.inbound.http.dto.CartResponse
import com.nexus.shopping.cart.adapter.inbound.http.dto.toCommand
import com.nexus.shopping.cart.adapter.inbound.http.dto.toResponse
import com.nexus.shopping.cart.application.usecase.AddCartItemUseCase
import com.nexus.shopping.cart.application.usecase.ClearCartUseCase
import com.nexus.shopping.cart.application.usecase.GetActiveCartByCustomerIdUseCase
import com.nexus.shopping.cart.application.usecase.RemoveCartItemUseCase
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Every endpoint here returns 200 with the current state of the cart, including the mutating ones
 * (POST/DELETE). Add/remove/clear operate on an existing resource (the customer's ACTIVE cart)
 * instead of creating a new top-level resource, so none of them warrants 201; returning the
 * updated cart body on every call (instead of 204) lets callers always render the latest state
 * without a follow-up GET.
 */
@RestController
@RequestMapping("/customers/{customerId}/cart")
class CartController(
    private val getActiveCartByCustomerIdUseCase: GetActiveCartByCustomerIdUseCase,
    private val addCartItemUseCase: AddCartItemUseCase,
    private val removeCartItemUseCase: RemoveCartItemUseCase,
    private val clearCartUseCase: ClearCartUseCase,
) {
    @GetMapping
    fun getActiveCart(
        @PathVariable customerId: Long,
    ): CartResponse = getActiveCartByCustomerIdUseCase.execute(customerId).toResponse()

    @PostMapping("/items")
    fun addItem(
        @PathVariable customerId: Long,
        @RequestBody request: AddCartItemRequest,
    ): CartResponse = addCartItemUseCase.execute(request.toCommand(customerId)).toResponse()

    @DeleteMapping("/items/{productId}")
    fun removeItem(
        @PathVariable customerId: Long,
        @PathVariable productId: Long,
    ): CartResponse = removeCartItemUseCase.execute(customerId, productId).toResponse()

    @DeleteMapping("/items")
    fun clearItems(
        @PathVariable customerId: Long,
    ): CartResponse = clearCartUseCase.execute(customerId).toResponse()
}
