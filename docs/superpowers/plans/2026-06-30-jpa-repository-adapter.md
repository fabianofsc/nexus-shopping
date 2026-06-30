# JPA Repository Adapter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Substituir o adapter outbound de produtos baseado em `JdbcTemplate` por um adapter JPA/Spring Data, preservando dominio puro, port existente e comportamento HTTP.

**Architecture:** O dominio e a aplicacao continuam sem imports de JPA. O pacote `product/adapter/outbound/jpa` passa a conter a entidade de persistencia, o repository Spring Data interno e o adapter que implementa `ProductRepositoryPort`. Consultas de leitura usam JPQL explicito com `@Query`; escritas usam `save`, entidade gerenciada e transacoes.

**Tech Stack:** Kotlin 2.2, Java 21, Spring Boot 4.1, Spring Data JPA, Hibernate, Flyway, H2 em testes, PostgreSQL em runtime, Gradle Wrapper.

---

## File Structure

- Modify: `build.gradle.kts`
  - Adicionar o plugin Kotlin JPA para construtor sem argumentos em entidades JPA.
- Delete: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jdbc/ProductRepository.kt`
  - Remover o bean antigo que implementa `ProductRepositoryPort` com `JdbcTemplate`.
- Create: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductEntity.kt`
  - Mapear a tabela `products` e converter entidade para dominio.
- Create: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/SpringDataProductRepository.kt`
  - Declarar o repository Spring Data interno e as queries JPQL de leitura.
- Create: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductJpaRepositoryAdapter.kt`
  - Implementar `ProductRepositoryPort` usando Spring Data JPA.
- Create: `src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductJpaRepositoryAdapterTest.kt`
  - Cobrir comportamento do adapter com H2 e Flyway.
- Modify: `src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt`
  - Ampliar a verificacao de nao vazamento de detalhes internos para excecoes JPA/Hibernate.

## Task 1: Preparar suporte Kotlin JPA

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Atualizar plugins**

Adicionar o plugin JPA ao bloco `plugins`:

```kotlin
plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}
```

- [ ] **Step 2: Verificar build script**

Run:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew help
```

Expected: comando termina com `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
rtk git add build.gradle.kts
rtk git commit -m "build: enable kotlin jpa plugin"
```

## Task 2: Criar teste inicial do adapter JPA

**Files:**
- Create: `src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductJpaRepositoryAdapterTest.kt`

- [ ] **Step 1: Criar teste falhando para o contrato do adapter**

Criar o arquivo:

```kotlin
package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.usecase.CreateProductCommand
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:product_jpa_repository_adapter_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=600",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class ProductJpaRepositoryAdapterTest {

    @Autowired
    private lateinit var repository: ProductJpaRepositoryAdapter

    @Test
    fun `findByCategoryId returns a slice with hasNext without count query`() {
        val result = repository.findByCategoryId(categoryId = 1L, page = 0, size = 1)

        assertEquals(0, result.page)
        assertEquals(1, result.size)
        assertEquals(1, result.count)
        assertTrue(result.hasNext)
        assertEquals(1L, result.content.single().categoryId)
        assertEquals(1L, result.content.single().id)
    }

    @Test
    fun `findByCategoryId returns second slice without hasNext`() {
        val result = repository.findByCategoryId(categoryId = 1L, page = 1, size = 1)

        assertEquals(1, result.page)
        assertEquals(1, result.size)
        assertEquals(1, result.count)
        assertFalse(result.hasNext)
        assertEquals(1L, result.content.single().categoryId)
        assertEquals(501L, result.content.single().id)
    }

    @Test
    fun `findByName uses prefix range bounds`() {
        val result = repository.findByName(name = "Product 1", page = 0, size = 3)

        assertEquals(0, result.page)
        assertEquals(3, result.size)
        assertEquals(3, result.count)
        assertTrue(result.hasNext)
        assertTrue(result.content.all { it.name.startsWith("Product 1") })
        assertEquals(listOf("Product 1", "Product 10", "Product 100"), result.content.map { it.name })
    }

    @Test
    fun `save persists product and returns generated values`() {
        val created = repository.save(
            CreateProductCommand(
                brandId = 1L,
                categoryId = 1L,
                sku = "SKU-JPA-001",
                name = "JPA Adapter Product",
                slug = "jpa-adapter-product",
                description = "Created by the JPA adapter test.",
                status = "ACTIVE",
                priceAmount = BigDecimal("49.90"),
                currency = "BRL",
                inventoryQuantity = 7,
            ),
        )

        assertTrue(created.id > 0)
        assertEquals("SKU-JPA-001", created.sku)
        assertEquals("JPA Adapter Product", created.name)
        assertEquals(0, BigDecimal("49.90").compareTo(created.priceAmount))
        assertNotNull(created.createdAt)
        assertNotNull(created.updatedAt)
    }

    @Test
    fun `updatePrice updates existing product`() {
        val updated = repository.updatePrice(id = 1L, priceAmount = BigDecimal("88.80"))

        assertNotNull(updated)
        assertEquals(1L, updated.id)
        assertEquals(0, BigDecimal("88.80").compareTo(updated.priceAmount))
    }

    @Test
    fun `updatePrice returns null when product does not exist`() {
        val updated = repository.updatePrice(id = 999999999L, priceAmount = BigDecimal("88.80"))

        assertNull(updated)
    }
}
```

- [ ] **Step 2: Rodar teste para confirmar falha**

Run:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests 'com.nexus.shopping.product.adapter.outbound.jpa.ProductJpaRepositoryAdapterTest'
```

Expected: FAIL porque `ProductJpaRepositoryAdapter` ainda nao existe.

- [ ] **Step 3: Commit do teste falhando**

```bash
rtk git add src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductJpaRepositoryAdapterTest.kt
rtk git commit -m "test: describe jpa product repository adapter"
```

## Task 3: Criar entidade JPA e mapper para dominio

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductEntity.kt`

- [ ] **Step 1: Criar entidade**

Criar o arquivo:

```kotlin
package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.domain.Product
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SourceType
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(name = "products")
class ProductEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,

    @Column(name = "brand_id", nullable = false)
    var brandId: Long = 0,

    @Column(name = "category_id", nullable = false)
    var categoryId: Long = 0,

    @Column(name = "sku", nullable = false, length = 120)
    var sku: String = "",

    @Column(name = "name", nullable = false, length = 220)
    var name: String = "",

    @Column(name = "slug", nullable = false, length = 260)
    var slug: String = "",

    @Column(name = "description", length = 2000)
    var description: String? = null,

    @Column(name = "status", nullable = false, length = 24)
    var status: String = "ACTIVE",

    @Column(name = "price_amount", nullable = false, precision = 12, scale = 2)
    var priceAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "BRL",

    @Column(name = "inventory_quantity", nullable = false)
    var inventoryQuantity: Int = 0,

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null,
) {
    fun toDomain(): Product =
        Product(
            id = requireNotNull(id) { "ProductEntity.id must be available before mapping to domain." },
            brandId = brandId,
            categoryId = categoryId,
            sku = sku,
            name = name,
            slug = slug,
            description = description,
            status = status,
            priceAmount = priceAmount,
            currency = currency,
            inventoryQuantity = inventoryQuantity,
            createdAt = requireNotNull(createdAt) { "ProductEntity.createdAt must be available before mapping to domain." },
            updatedAt = requireNotNull(updatedAt) { "ProductEntity.updatedAt must be available before mapping to domain." },
        )
}
```

- [ ] **Step 2: Rodar teste do adapter**

Run:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests 'com.nexus.shopping.product.adapter.outbound.jpa.ProductJpaRepositoryAdapterTest'
```

Expected: FAIL porque o repository Spring Data e o adapter JPA ainda nao existem.

- [ ] **Step 3: Commit**

```bash
rtk git add src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductEntity.kt
rtk git commit -m "feat: map product jpa entity"
```

## Task 4: Criar Spring Data repository com queries JPQL

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/SpringDataProductRepository.kt`

- [ ] **Step 1: Criar repository interno**

Criar o arquivo:

```kotlin
package com.nexus.shopping.product.adapter.outbound.jpa

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpringDataProductRepository : JpaRepository<ProductEntity, Long> {

    @Query(
        """
        SELECT p FROM ProductEntity p
        WHERE p.categoryId = :categoryId
        ORDER BY p.id
        """,
    )
    fun findByCategoryId(
        @Param("categoryId") categoryId: Long,
        pageable: Pageable,
    ): List<ProductEntity>

    @Query(
        """
        SELECT p FROM ProductEntity p
        WHERE p.name >= :name
          AND p.name < :upperBound
          AND p.name LIKE :prefix
        ORDER BY p.name
        """,
    )
    fun findByNamePrefix(
        @Param("name") name: String,
        @Param("upperBound") upperBound: String,
        @Param("prefix") prefix: String,
        pageable: Pageable,
    ): List<ProductEntity>
}
```

- [ ] **Step 2: Rodar teste do adapter**

Run:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests 'com.nexus.shopping.product.adapter.outbound.jpa.ProductJpaRepositoryAdapterTest'
```

Expected: FAIL porque `ProductJpaRepositoryAdapter` ainda nao existe.

- [ ] **Step 3: Commit**

```bash
rtk git add src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/SpringDataProductRepository.kt
rtk git commit -m "feat: add product spring data queries"
```

## Task 5: Implementar adapter JPA e remover adapter JDBC

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductJpaRepositoryAdapter.kt`
- Delete: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jdbc/ProductRepository.kt`

- [ ] **Step 1: Criar adapter JPA**

Criar o arquivo:

```kotlin
package com.nexus.shopping.product.adapter.outbound.jpa

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.application.usecase.CreateProductCommand
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import java.math.BigDecimal
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class ProductJpaRepositoryAdapter(
    private val repository: SpringDataProductRepository,
) : ProductRepositoryPort {

    @Transactional(readOnly = true)
    override fun findByCategoryId(categoryId: Long, page: Int, size: Int): ProductPage {
        val products = repository.findByCategoryId(
            categoryId = categoryId,
            pageable = PageRequest.of(page, size + 1),
        ).map { it.toDomain() }

        return products.toPage(page, size)
    }

    @Transactional(readOnly = true)
    override fun findByName(name: String, page: Int, size: Int): ProductPage {
        val products = repository.findByNamePrefix(
            name = name,
            upperBound = nextLexicographicValue(name),
            prefix = "$name%",
            pageable = PageRequest.of(page, size + 1),
        ).map { it.toDomain() }

        return products.toPage(page, size)
    }

    @Transactional
    override fun save(command: CreateProductCommand): Product {
        val saved = repository.saveAndFlush(
            ProductEntity(
                brandId = command.brandId,
                categoryId = command.categoryId,
                sku = command.sku,
                name = command.name,
                slug = command.slug,
                description = command.description,
                status = command.status,
                priceAmount = command.priceAmount,
                currency = command.currency,
                inventoryQuantity = command.inventoryQuantity,
            ),
        )

        return saved.toDomain()
    }

    @Transactional
    override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? {
        val product = repository.findById(id).orElse(null) ?: return null

        product.priceAmount = priceAmount

        return repository.saveAndFlush(product).toDomain()
    }

    private fun List<Product>.toPage(page: Int, size: Int): ProductPage {
        val hasNext = this.size > size
        val content = if (hasNext) take(size) else this

        return ProductPage(
            content = content,
            page = page,
            size = size,
            count = content.size,
            hasNext = hasNext,
        )
    }

    private fun nextLexicographicValue(value: String): String {
        val chars = value.toCharArray()
        for (index in chars.indices.reversed()) {
            if (chars[index] != Char.MAX_VALUE) {
                chars[index] = (chars[index].code + 1).toChar()
                return chars.concatToString(endIndex = index + 1)
            }
        }
        return value
    }
}
```

- [ ] **Step 2: Remover adapter JDBC antigo**

Delete:

```text
src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jdbc/ProductRepository.kt
```

- [ ] **Step 3: Rodar teste do adapter**

Run:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests 'com.nexus.shopping.product.adapter.outbound.jpa.ProductJpaRepositoryAdapterTest'
```

Expected: PASS para `ProductJpaRepositoryAdapterTest`.

- [ ] **Step 4: Verificar que nao sobrou adapter JDBC**

Run:

```bash
rtk rg "JdbcTemplate|SimpleJdbcInsert|adapter.outbound.jdbc" src/main/kotlin src/test/kotlin
```

Expected: nenhum resultado em `src/main/kotlin`. Resultados em documentacao nao importam para esta tarefa.

- [ ] **Step 5: Commit**

```bash
rtk git add src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductJpaRepositoryAdapter.kt src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductEntity.kt src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/SpringDataProductRepository.kt src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductJpaRepositoryAdapterTest.kt
rtk git rm src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jdbc/ProductRepository.kt
rtk git commit -m "feat: replace product jdbc adapter with jpa"
```

## Task 6: Preservar contrato HTTP de erro sem vazamento

**Files:**
- Modify: `src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt`

- [ ] **Step 1: Ampliar helper de vazamento**

Atualizar `assertNoInternalDetailsLeaked` para:

```kotlin
private fun assertNoInternalDetailsLeaked(body: String) {
    assertFalse(body.contains("DataIntegrityViolationException"))
    assertFalse(body.contains("JdbcSQLIntegrityConstraintViolationException"))
    assertFalse(body.contains("Referential integrity"))
    assertFalse(body.contains("ConstraintViolationException"))
    assertFalse(body.contains("org.hibernate"))
    assertFalse(body.contains("jakarta.persistence"))
    assertFalse(body.contains("constraint", ignoreCase = true))
    assertFalse(body.contains("org.springframework"))
    assertFalse(body.contains("java.sql"))
    assertFalse(body.contains("\tat "))
}
```

- [ ] **Step 2: Rodar teste HTTP do contrato de produto**

Run:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests 'com.nexus.shopping.ProductControllerTest'
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
rtk git add src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt
rtk git commit -m "test: guard jpa persistence error details"
```

## Task 7: Verificacoes de arquitetura e build completo

**Files:**
- Nenhum arquivo de codigo e modificado nesta tarefa.

- [ ] **Step 1: Confirmar que dominio e aplicacao continuam puros**

Run:

```bash
rtk rg "jakarta.persistence|org.hibernate|org.springframework.data" src/main/kotlin/com/nexus/shopping/product/domain src/main/kotlin/com/nexus/shopping/product/application
```

Expected: nenhum resultado.

- [ ] **Step 2: Confirmar queries JPQL explicitas**

Run:

```bash
rtk rg "@Query|SELECT p FROM ProductEntity" src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa
```

Expected: resultado em `SpringDataProductRepository.kt` mostrando as duas queries de leitura.

- [ ] **Step 3: Rodar build completo**

Run:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Revisar diff**

Run:

```bash
rtk git diff --stat HEAD
rtk git diff HEAD -- build.gradle.kts src/main/kotlin src/test/kotlin
```

Expected: diff restrito ao plugin JPA, remocao do adapter JDBC, novo adapter JPA e ajuste de teste HTTP.

- [ ] **Step 5: Commit se houver ajustes finais**

Se os passos anteriores exigirem ajustes pequenos, commitar somente esses ajustes:

```bash
rtk git add build.gradle.kts src/main/kotlin src/test/kotlin
rtk git commit -m "chore: finalize jpa repository adapter refactor"
```

## Self-Review

- Spec coverage: as tarefas cobrem dominio puro, entidade separada, queries JPQL, escrita JPA natural, paginacao `size + 1`, testes do adapter, contrato HTTP e verificacao arquitetural.
- Placeholder scan: o plano nao contem marcadores abertos.
- Type consistency: nomes planejados sao `ProductEntity`, `SpringDataProductRepository`, `ProductJpaRepositoryAdapter` e preservam `ProductRepositoryPort`.
- Scope check: o plano e focado em um unico adapter outbound e nao inclui entidades de marca/categoria, Bean Validation, mudancas HTTP ou benchmark.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-30-jpa-repository-adapter.md`. Duas opcoes para uma execucao futura, quando o usuario autorizar:

1. Subagent-Driven (recommended) - despachar um subagente fresco por tarefa, revisar entre tarefas e iterar rapido.
2. Inline Execution - executar as tarefas nesta sessao usando executing-plans, com checkpoints.
