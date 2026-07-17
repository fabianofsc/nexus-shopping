package com.nexus.shopping.customer.adapter.outbound.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface SpringDataCustomerRepository : JpaRepository<CustomerEntity, Long> {
    @Query(
        """
        SELECT c FROM CustomerEntity c
        WHERE c.id = :id
        """,
    )
    fun findCustomerById(
        @Param("id") id: Long,
    ): Optional<CustomerEntity>
}
