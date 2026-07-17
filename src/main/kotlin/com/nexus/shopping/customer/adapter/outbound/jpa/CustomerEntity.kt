package com.nexus.shopping.customer.adapter.outbound.jpa

import com.nexus.shopping.customer.application.command.CreateCustomerCommand
import com.nexus.shopping.customer.domain.Address
import com.nexus.shopping.customer.domain.Contact
import com.nexus.shopping.customer.domain.Customer
import com.nexus.shopping.customer.domain.CustomerStatus
import com.nexus.shopping.customer.domain.DocumentType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SourceType
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "customers")
class CustomerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "name", nullable = false, length = 160)
    var name: String = "",
    @Column(name = "document", nullable = false, length = 32)
    var document: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 8)
    var documentType: DocumentType = DocumentType.CPF,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    var status: CustomerStatus = CustomerStatus.ACTIVE,
    @OneToOne(mappedBy = "customer", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, optional = false)
    var contact: CustomerContactEntity? = null,
    @OneToOne(mappedBy = "customer", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, optional = false)
    var address: CustomerAddressEntity? = null,
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,
    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null,
) {
    fun toDomain(): Customer =
        Customer(
            id = requireNotNull(id) { "CustomerEntity.id must be available before mapping to domain." },
            name = name,
            document = document,
            documentType = documentType,
            status = status,
            contact = requireNotNull(contact) { "CustomerEntity.contact must be available before mapping to domain." }.toDomain(),
            address = requireNotNull(address) { "CustomerEntity.address must be available before mapping to domain." }.toDomain(),
            createdAt = requireNotNull(createdAt) { "CustomerEntity.createdAt must be available before mapping to domain." },
            updatedAt = requireNotNull(updatedAt) { "CustomerEntity.updatedAt must be available before mapping to domain." },
        )
}

@Entity
@Table(name = "customer_contacts")
class CustomerContactEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: CustomerEntity? = null,
    @Column(name = "email", nullable = false, length = 254)
    var email: String = "",
    @Column(name = "phone", length = 32)
    var phone: String? = null,
) {
    fun toDomain(): Contact =
        Contact(
            email = email,
            phone = phone,
        )
}

@Entity
@Table(name = "customer_addresses")
class CustomerAddressEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: CustomerEntity? = null,
    @Column(name = "street", nullable = false, length = 180)
    var street: String = "",
    @Column(name = "number", nullable = false, length = 40)
    var number: String = "",
    @Column(name = "complement", length = 120)
    var complement: String? = null,
    @Column(name = "neighborhood", nullable = false, length = 120)
    var neighborhood: String = "",
    @Column(name = "city", nullable = false, length = 120)
    var city: String = "",
    @Column(name = "state", nullable = false, length = 60)
    var state: String = "",
    @Column(name = "zip_code", nullable = false, length = 20)
    var zipCode: String = "",
    @Column(name = "country", nullable = false, length = 2)
    var country: String = "",
) {
    fun toDomain(): Address =
        Address(
            street = street,
            number = number,
            complement = complement,
            neighborhood = neighborhood,
            city = city,
            state = state,
            zipCode = zipCode,
            country = country,
        )
}

fun CreateCustomerCommand.toEntity(): CustomerEntity {
    val customer =
        CustomerEntity(
            name = name,
            document = document,
            documentType = DocumentType.valueOf(documentType),
            status = CustomerStatus.ACTIVE,
        )
    customer.contact =
        CustomerContactEntity(
            customer = customer,
            email = email,
            phone = phone,
        )
    customer.address =
        CustomerAddressEntity(
            customer = customer,
            street = street,
            number = number,
            complement = complement,
            neighborhood = neighborhood,
            city = city,
            state = state,
            zipCode = zipCode,
            country = country,
        )
    return customer
}
