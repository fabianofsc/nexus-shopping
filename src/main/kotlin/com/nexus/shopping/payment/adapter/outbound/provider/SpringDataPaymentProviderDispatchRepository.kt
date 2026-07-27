package com.nexus.shopping.payment.adapter.outbound.provider

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface SpringDataPaymentProviderDispatchRepository : JpaRepository<PaymentProviderDispatchEntity, Long> {
    @Query(
        """
        SELECT d FROM PaymentProviderDispatchEntity d
        WHERE d.providerDispatchKey = :providerDispatchKey
        """,
    )
    fun findByProviderDispatchKey(
        @Param("providerDispatchKey") providerDispatchKey: String,
    ): Optional<PaymentProviderDispatchEntity>
}
