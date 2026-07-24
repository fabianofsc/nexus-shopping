package com.nexus.shopping.order.adapter.inbound.http

import com.nexus.shopping.order.application.port.outbound.CartCheckoutPort
import com.nexus.shopping.order.application.port.outbound.OrderRepositoryPort
import com.nexus.shopping.order.application.port.outbound.TransactionPort
import com.nexus.shopping.order.application.usecase.CancelOrderUseCase
import com.nexus.shopping.order.application.usecase.CheckoutOrderUseCase
import com.nexus.shopping.order.application.usecase.GetOrderByIdUseCase
import com.nexus.shopping.order.application.usecase.ListOrdersByCustomerUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OrderApplicationConfiguration {
    @Bean
    fun checkoutOrderUseCase(
        orderRepository: OrderRepositoryPort,
        cartCheckout: CartCheckoutPort,
        transaction: TransactionPort,
    ) = CheckoutOrderUseCase(orderRepository, cartCheckout, transaction)

    @Bean
    fun getOrderByIdUseCase(orderRepository: OrderRepositoryPort) = GetOrderByIdUseCase(orderRepository)

    @Bean
    fun listOrdersByCustomerUseCase(orderRepository: OrderRepositoryPort) = ListOrdersByCustomerUseCase(orderRepository)

    @Bean
    fun cancelOrderUseCase(
        orderRepository: OrderRepositoryPort,
        transaction: TransactionPort,
    ) = CancelOrderUseCase(orderRepository, transaction)
}
