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
- Modify: `README.md`
  - Atualizar a stack, a arvore arquitetural e a narrativa do repository para JPA/Spring Data.
- Modify: `AGENTS.md`
  - Atualizar as instrucoes de arquitetura e stack para o adapter JPA, mantendo o arquivo abaixo de 200 linhas.

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
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew help
```

Expected: comando termina com `BUILD SUCCESSFUL`.

- [ ] **Step 3: Revisar diff local**

```bash
git diff -- build.gradle.kts
```

Expected: diff mostra apenas a inclusao de `kotlin("plugin.jpa")`.

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
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests 'com.nexus.shopping.product.adapter.outbound.jpa.ProductJpaRepositoryAdapterTest'
```

Expected: FAIL porque `ProductJpaRepositoryAdapter` ainda nao existe.

- [ ] **Step 3: Revisar diff local**

```bash
git diff -- src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductJpaRepositoryAdapterTest.kt
```

Expected: diff mostra apenas o novo teste do adapter JPA.

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

    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
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
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests 'com.nexus.shopping.product.adapter.outbound.jpa.ProductJpaRepositoryAdapterTest'
```

Expected: FAIL porque o repository Spring Data e o adapter JPA ainda nao existem.

- [ ] **Step 3: Revisar diff local**

```bash
git diff -- src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductEntity.kt
```

Expected: diff mostra apenas a nova entidade JPA, com `currency` mapeado como `CHAR(3)`.

## Task 4: Criar Spring Data repository com queries JPQL

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/SpringDataProductRepository.kt`

- [ ] **Step 1: Criar repository interno**

Criar o arquivo:

```kotlin
package com.nexus.shopping.product.adapter.outbound.jpa

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
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
    ): Slice<ProductEntity>

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
    ): Slice<ProductEntity>
}
```

- [ ] **Step 2: Rodar teste do adapter**

Run:

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests 'com.nexus.shopping.product.adapter.outbound.jpa.ProductJpaRepositoryAdapterTest'
```

Expected: FAIL porque `ProductJpaRepositoryAdapter` ainda nao existe.

- [ ] **Step 3: Revisar diff local**

```bash
git diff -- src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/SpringDataProductRepository.kt
```

Expected: diff mostra o repository Spring Data com duas queries JPQL retornando `Slice<ProductEntity>`.

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
import com.nexus.shopping.product.domain.ProductPage
import java.math.BigDecimal
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
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
            pageable = PageRequest.of(page, size),
        )

        return products.toProductPage(page, size)
    }

    @Transactional(readOnly = true)
    override fun findByName(name: String, page: Int, size: Int): ProductPage {
        val products = repository.findByNamePrefix(
            name = name,
            upperBound = nextLexicographicValue(name),
            prefix = "$name%",
            pageable = PageRequest.of(page, size),
        )

        return products.toProductPage(page, size)
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

    private fun Slice<ProductEntity>.toProductPage(page: Int, size: Int): ProductPage {
        val productContent = content.map { it.toDomain() }

        return ProductPage(
            content = productContent,
            page = page,
            size = size,
            count = productContent.size,
            hasNext = hasNext(),
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
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests 'com.nexus.shopping.product.adapter.outbound.jpa.ProductJpaRepositoryAdapterTest'
```

Expected: PASS para `ProductJpaRepositoryAdapterTest`.

- [ ] **Step 4: Verificar que nao sobrou adapter JDBC**

Run:

```bash
rg "JdbcTemplate|SimpleJdbcInsert|adapter.outbound.jdbc" src/main/kotlin src/test/kotlin
```

Expected: nenhum resultado em `src/main/kotlin`. Resultados em documentacao nao importam para esta tarefa.

- [ ] **Step 5: Revisar diff local**

```bash
git status --short
git diff -- src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jdbc src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa
```

Expected: diff mostra o novo adapter JPA, o novo repository Spring Data, a nova entidade, o novo teste e a remocao do adapter JDBC. Nao executar commit sem autorizacao explicita do usuario.

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
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests 'com.nexus.shopping.ProductControllerTest'
```

Expected: PASS.

- [ ] **Step 3: Revisar diff local**

```bash
git diff -- src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt
```

Expected: diff mostra apenas a ampliacao do helper de vazamento de detalhes internos.

## Task 7: Atualizar documentacao do projeto

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: Localizar referencias antigas a JDBC/JPA rejeitado**

Run:

```bash
rg -n "JdbcTemplate|SimpleJdbcInsert|JPA/ORM rejeitado|sem JPA/ORM|adapter/outbound/jdbc|outbound/jdbc" README.md AGENTS.md
```

Expected: resultados apontam os trechos que ainda descrevem o adapter JDBC como implementacao atual.

- [ ] **Step 2: Atualizar README.md**

Atualizar os trechos relevantes para refletir o novo estado:

```markdown
# Nexus Shopping

Backend REST API educacional construido com Kotlin, Java 21, Spring Boot 4, Actuator, Flyway, PostgreSQL e Spring Data JPA.
```

Atualizar a arvore de arquitetura para:

```text
product/
  domain/ -> Product, ProductPage
  application/ -> use cases e ProductRepositoryPort
  adapter/
    inbound/http/ -> ProductController e DTOs HTTP
    outbound/jpa/ -> ProductJpaRepositoryAdapter, ProductEntity, SpringDataProductRepository
```

Atualizar a explicacao do repository para preservar a intencao didatica:

```markdown
O dominio permanece livre de anotacoes de framework. A persistencia JPA fica isolada no adapter outbound, que implementa `ProductRepositoryPort`, mapeia `ProductEntity` para `Product` e usa `@Query` JPQL nas consultas de leitura para manter explicito o shape das queries de performance.
```

- [ ] **Step 3: Atualizar AGENTS.md**

Atualizar o snapshot da stack para citar Spring Data JPA:

```markdown
- Stack: Kotlin, Java 21, Gradle Wrapper, Spring Boot 4, Actuator, Flyway, PostgreSQL, Spring Data JPA.
```

Atualizar a arvore do adapter outbound:

```text
  adapter/
    inbound/http/
      ProductController.kt             # HTTP -> use case -> HTTP
      CreateProductRequest.kt          # DTO HTTP com toCommand()
    outbound/jpa/
      ProductEntity.kt                 # entidade JPA isolada no adapter
      SpringDataProductRepository.kt   # Spring Data repository com @Query JPQL para leituras
      ProductJpaRepositoryAdapter.kt   # implementa ProductRepositoryPort via JPA
```

Substituir a decisao antiga de rejeicao de JPA por:

```markdown
- JPA/ORM e aceito somente no adapter outbound; dominio e application continuam sem anotacoes ou imports de persistencia.
- Consultas de leitura usam `@Query` JPQL para manter visivel o shape das queries e o valor didatico de performance.
- Escritas usam o fluxo natural do JPA (`save`, entidade gerenciada e dirty checking).
```

- [ ] **Step 4: Validar que nao sobraram contradicoes na documentacao**

Run:

```bash
rg -n "JdbcTemplate|SimpleJdbcInsert|JPA/ORM rejeitado|sem JPA/ORM|adapter/outbound/jdbc|outbound/jdbc" README.md AGENTS.md
```

Expected: nenhum resultado, exceto se houver mencao historica claramente marcada como passado. Preferir nenhum resultado.

- [ ] **Step 5: Validar tamanho do AGENTS.md**

Run:

```bash
wc -l AGENTS.md
```

Expected: menos de 200 linhas.

- [ ] **Step 6: Revisar diff local**

Run:

```bash
git diff -- README.md AGENTS.md
```

Expected: diff restrito a atualizar a documentacao para o adapter JPA, sem mudar comandos locais, regras de branch ou secoes nao relacionadas.

## Task 8: Verificacoes de arquitetura e build completo

**Files:**
- Nenhum arquivo de codigo e modificado nesta tarefa.

- [ ] **Step 1: Confirmar que dominio e aplicacao continuam puros**

Run:

```bash
rg "jakarta.persistence|org.hibernate|org.springframework.data" src/main/kotlin/com/nexus/shopping/product/domain src/main/kotlin/com/nexus/shopping/product/application
```

Expected: nenhum resultado.

- [ ] **Step 2: Confirmar queries JPQL explicitas**

Run:

```bash
rg "@Query|SELECT p FROM ProductEntity" src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa
```

Expected: resultado em `SpringDataProductRepository.kt` mostrando as duas queries de leitura.

- [ ] **Step 3: Rodar build completo**

Run:

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Revisar diff**

Run:

```bash
git diff --stat HEAD
git diff HEAD -- build.gradle.kts src/main/kotlin src/test/kotlin README.md AGENTS.md
```

Expected: diff restrito ao plugin JPA, remocao do adapter JDBC, novo adapter JPA, ajuste de teste HTTP e documentacao do adapter JPA.

- [ ] **Step 5: Preparar handoff sem commit**

Se os passos anteriores exigirem ajustes pequenos, revisar o status final e aguardar autorizacao explicita antes de qualquer commit:

```bash
git status --short
```

## Self-Review

- Spec coverage: as tarefas cobrem dominio puro, entidade separada, queries JPQL, escrita JPA natural, paginacao com `Slice` sem `COUNT(*)`, testes do adapter, contrato HTTP, documentacao do projeto e verificacao arquitetural.
- Placeholder scan: o plano nao contem marcadores abertos.
- Type consistency: nomes planejados sao `ProductEntity`, `SpringDataProductRepository`, `ProductJpaRepositoryAdapter` e preservam `ProductRepositoryPort`.
- Scope check: o plano e focado em um unico adapter outbound e nao inclui entidades de marca/categoria, Bean Validation, mudancas HTTP ou benchmark.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-30-jpa-repository-adapter.md`. Duas opcoes para uma execucao futura, quando o usuario autorizar:

1. Subagent-Driven (recommended) - despachar um subagente fresco por tarefa, revisar entre tarefas e iterar rapido.
2. Inline Execution - executar as tarefas nesta sessao usando executing-plans, com checkpoints.
