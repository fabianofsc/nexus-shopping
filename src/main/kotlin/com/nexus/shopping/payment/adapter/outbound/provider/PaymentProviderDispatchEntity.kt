package com.nexus.shopping.payment.adapter.outbound.provider

import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingResult
import com.nexus.shopping.payment.domain.PaymentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "payment_provider_dispatches")
class PaymentProviderDispatchEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "provider_dispatch_key", nullable = false, unique = true, length = 255)
    var providerDispatchKey: String = "",
    @Column(name = "reference_id", nullable = false, length = 255)
    var referenceId: String = "",
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: PaymentStatus = PaymentStatus.REJECTED,
    @Column(name = "provider_transaction_id", length = 255)
    var providerTransactionId: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
) {
    fun toResult(): ProviderProcessingResult =
        ProviderProcessingResult(
            status = status,
            providerTransactionId = providerTransactionId,
        )
}
