package com.nexus.shopping.order.domain

class OrderStateTransitionException(
    status: OrderStatus,
) : IllegalStateException("Order in status $status cannot be cancelled.")
