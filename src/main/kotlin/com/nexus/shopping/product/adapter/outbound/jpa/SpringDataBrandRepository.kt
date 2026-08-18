package com.nexus.shopping.product.adapter.outbound.jpa

import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataBrandRepository : JpaRepository<BrandEntity, Long>
