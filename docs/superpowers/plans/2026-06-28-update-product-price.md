# Update Product Price — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `PATCH /products/{id}` that updates only `priceAmount`, returning the full updated product or 400/404 on error.

**Architecture:** Hexagonal — new `UpdateProductPriceUseCase` orchestrates validation and calls `ProductRepositoryPort.updatePrice`, which executes a portable `UPDATE` followed by a `SELECT` when a row is changed. Errors are typed exceptions caught by the controller (same pattern as the existing `POST /products` flow).

**Tech Stack:** Kotlin, Spring Boot 4, JdbcTemplate, PostgreSQL / H2 (tests)

## Global Constraints

- All shell commands must be prefixed with `rtk`
- Use Gradle wrapper: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build`
- No JPA/ORM — JDBC only
- Unit test style: `kotlin.test` (`assertFailsWith`, `assertEquals`), hand-rolled fakes — no Mockito
- Controller test style: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + raw `java.net.http.HttpClient` (same as `ProductControllerTest`)
- SQL must be portable between PostgreSQL and H2
- Dependency direction: adapter → application → domain

---

## File Map

| Action | File |
|---|---|
| Create | `src/main/kotlin/com/nexus/shopping/product/application/usecase/UpdatePriceCommand.kt` |
| Create | `src/main/kotlin/com/nexus/shopping/product/application/usecase/ProductNotFoundException.kt` |
| Create | `src/main/kotlin/com/nexus/shopping/product/application/usecase/UpdateProductPriceUseCase.kt` |
| Create | `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/UpdatePriceRequest.kt` |
| Create | `src/test/kotlin/com/nexus/shopping/product/UpdateProductPriceUseCaseTest.kt` |
| Modify | `src/main/kotlin/com/nexus/shopping/product/application/port/outbound/ProductRepositoryPort.kt` |
| Modify | `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jdbc/ProductRepository.kt` |
| Modify | `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductController.kt` |
| Modify | `src/test/kotlin/com/nexus/shopping/product/ProductSearchUseCaseTest.kt` |
| Modify | `src/test/kotlin/com/nexus/shopping/product/ProductCreateUseCaseTest.kt` |
| Modify | `src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt` |

---

## Task 1: Foundation types and port contract

> **Context:** The port currently has `findByCategoryId`, `findByName`, and `save`. Adding `updatePrice` requires updating every hand-rolled fake that implements the port (`ProductSearchUseCaseTest`, `ProductCreateUseCaseTest`).

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/product/application/usecase/UpdatePriceCommand.kt`
- Create: `src/main/kotlin/com/nexus/shopping/product/application/usecase/ProductNotFoundException.kt`
- Modify: `src/main/kotlin/com/nexus/shopping/product/application/port/outbound/ProductRepositoryPort.kt`
- Modify: `src/test/kotlin/com/nexus/shopping/product/ProductSearchUseCaseTest.kt`
- Modify: `src/test/kotlin/com/nexus/shopping/product/ProductCreateUseCaseTest.kt`

**Interfaces:**
- Produces: `UpdatePriceCommand(id: Long, priceAmount: BigDecimal)`, `ProductNotFoundException`, `ProductRepositoryPort.updatePrice(id: Long, priceAmount: BigDecimal): Product?`

- [x] **Step 1: Create `UpdatePriceCommand.kt`**

```kotlin
package com.nexus.shopping.product.application.usecase

import java.math.BigDecimal

data class UpdatePriceCommand(
    val id: Long,
    val priceAmount: BigDecimal,
)
```

- [x] **Step 2: Create `ProductNotFoundException.kt`**

```kotlin
package com.nexus.shopping.product.application.usecase

class ProductNotFoundException(message: String) : RuntimeException(message)
```

- [x] **Step 3: Add `updatePrice` to `ProductRepositoryPort`**

Replace the entire file — keep the existing `save` method, add `updatePrice`:

```kotlin
package com.nexus.shopping.product.application.port.outbound

import com.nexus.shopping.product.application.usecase.CreateProductCommand
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import java.math.BigDecimal

interface ProductRepositoryPort {
    fun findByCategoryId(categoryId: Long, page: Int, size: Int): ProductPage
    fun findByName(name: String, page: Int, size: Int): ProductPage
    fun save(command: CreateProductCommand): Product
    fun updatePrice(id: Long, priceAmount: BigDecimal): Product?
}
```

- [x] **Step 4: Add `updatePrice` stub to `ProductSearchUseCaseTest` fake**

The fake must implement every port method. Add the `updatePrice` stub and the `BigDecimal` import to the existing file:

```kotlin
package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProductSearchUseCaseTest {

    private var lastCalledMethod: String? = null

    private val fakeRepo = object : ProductRepositoryPort {
        override fun findByCategoryId(categoryId: Long, page: Int, size: Int): ProductPage {
            lastCalledMethod = "findByCategoryId"
            return ProductPage(content = emptyList(), page = page, size = size, count = 0, hasNext = false)
        }

        override fun findByName(name: String, page: Int, size: Int): ProductPage {
            lastCalledMethod = "findByName"
            return ProductPage(content = emptyList(), page = page, size = size, count = 0, hasNext = false)
        }

        override fun save(command: CreateProductCommand): Product = throw UnsupportedOperationException()

        override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? = throw UnsupportedOperationException()
    }

    private val useCase = ProductSearchUseCase(fakeRepo)

    @Test
    fun `search by categoryId delegates to findByCategoryId`() {
        val result = useCase.search(categoryId = 42L, name = null, page = 0, size = 10)
        assertEquals("findByCategoryId", lastCalledMethod)
        assertEquals(0, result.page)
        assertEquals(10, result.size)
    }

    @Test
    fun `search by name delegates to findByName`() {
        val result = useCase.search(categoryId = null, name = "notebook", page = 1, size = 20)
        assertEquals("findByName", lastCalledMethod)
        assertEquals(1, result.page)
        assertEquals(20, result.size)
    }

    @Test
    fun `search with both categoryId and name throws ProductValidationException`() {
        assertFailsWith<ProductValidationException> {
            useCase.search(categoryId = 1L, name = "notebook", page = 0, size = 10)
        }
    }

    @Test
    fun `search with neither categoryId nor name throws ProductValidationException`() {
        assertFailsWith<ProductValidationException> {
            useCase.search(categoryId = null, name = null, page = 0, size = 10)
        }
    }

    @Test
    fun `search with negative page throws ProductValidationException`() {
        assertFailsWith<ProductValidationException> {
            useCase.search(categoryId = 1L, name = null, page = -1, size = 10)
        }
    }

    @Test
    fun `search with size out of range throws ProductValidationException`() {
        assertFailsWith<ProductValidationException> {
            useCase.search(categoryId = 1L, name = null, page = 0, size = 501)
        }
    }
}
```

- [x] **Step 5: Add `updatePrice` stub to `ProductCreateUseCaseTest` fake**

Add the import and the stub — only the fake block changes, all tests remain intact:

```kotlin
package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProductCreateUseCaseTest {

    private val fakeRepo = object : ProductRepositoryPort {
        override fun findByCategoryId(categoryId: Long, page: Int, size: Int) =
            ProductPage(content = emptyList(), page = page, size = size, count = 0, hasNext = false)

        override fun findByName(name: String, page: Int, size: Int) =
            ProductPage(content = emptyList(), page = page, size = size, count = 0, hasNext = false)

        override fun save(command: CreateProductCommand): Product = Product(
            id = 1L, brandId = command.brandId, categoryId = command.categoryId,
            sku = "SKU-TEST", name = command.name, slug = command.slug,
            description = command.description, status = command.status,
            priceAmount = command.priceAmount, currency = command.currency,
            inventoryQuantity = command.inventoryQuantity,
            createdAt = java.time.LocalDateTime.now(), updatedAt = java.time.LocalDateTime.now(),
        )

        override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? = throw UnsupportedOperationException()
    }

    private val useCase = ProductCreateUseCase(fakeRepo)

    private fun validCommand() = CreateProductCommand(
        brandId = 1L,
        categoryId = 1L,
        sku = "SKU-001",
        name = "Product Name",
        slug = "product-name",
        priceAmount = BigDecimal("29.90"),
    )

    @Test
    fun `create valid product delegates to repository`() {
        val result = useCase.create(validCommand())
        assertEquals("SKU-TEST", result.sku)
    }

    @Test
    fun `create with blank sku throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(sku = "  "))
        }
    }

    @Test
    fun `create with sku over 120 chars throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(sku = "a".repeat(121)))
        }
    }

    @Test
    fun `create with blank name throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(name = ""))
        }
    }

    @Test
    fun `create with name over 220 chars throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(name = "a".repeat(221)))
        }
    }

    @Test
    fun `create with blank slug throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(slug = ""))
        }
    }

    @Test
    fun `create with description over 2000 chars throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(description = "a".repeat(2001)))
        }
    }

    @Test
    fun `create with invalid status throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(status = "DELETED"))
        }
    }

    @Test
    fun `create with negative priceAmount throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(priceAmount = BigDecimal("-0.01")))
        }
    }

    @Test
    fun `create with currency length not 3 throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(currency = "US"))
        }
    }

    @Test
    fun `create with negative inventoryQuantity throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(inventoryQuantity = -1))
        }
    }

    @Test
    fun `create with zero brandId throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(brandId = 0L))
        }
    }

    @Test
    fun `create with zero categoryId throws`() {
        assertFailsWith<ProductValidationException> {
            useCase.create(validCommand().copy(categoryId = 0L))
        }
    }
}
```

- [x] **Step 6: Run build — expect BUILD SUCCESSFUL**

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Expected: `BUILD SUCCESSFUL` — all existing tests pass. Build will flag `ProductRepository` for not implementing `updatePrice` yet; that is expected and will be fixed in Task 3.

> If the build fails for any reason OTHER than `ProductRepository` missing `updatePrice`, fix it before continuing.

- [x] **Step 7: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/product/application/usecase/UpdatePriceCommand.kt
git add src/main/kotlin/com/nexus/shopping/product/application/usecase/ProductNotFoundException.kt
git add src/main/kotlin/com/nexus/shopping/product/application/port/outbound/ProductRepositoryPort.kt
git add src/test/kotlin/com/nexus/shopping/product/ProductSearchUseCaseTest.kt
git add src/test/kotlin/com/nexus/shopping/product/ProductCreateUseCaseTest.kt
git commit -m "feat: add UpdatePriceCommand, ProductNotFoundException, and port contract"
```

---

## Task 2: UpdateProductPriceUseCase (TDD)

**Files:**
- Create: `src/test/kotlin/com/nexus/shopping/product/UpdateProductPriceUseCaseTest.kt`
- Create: `src/main/kotlin/com/nexus/shopping/product/application/usecase/UpdateProductPriceUseCase.kt`

**Interfaces:**
- Consumes: `UpdatePriceCommand(id, priceAmount)`, `ProductNotFoundException`, `ProductValidationException`, `ProductRepositoryPort.updatePrice`
- Produces: `UpdateProductPriceUseCase.execute(command: UpdatePriceCommand): Product`

- [x] **Step 1: Write the failing test**

The fake must implement all four port methods (`findByCategoryId`, `findByName`, `save`, `updatePrice`):

```kotlin
package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateProductPriceUseCaseTest {

    private fun aProduct(price: BigDecimal) = Product(
        id = 1L,
        brandId = 1L,
        categoryId = 1L,
        sku = "SKU-001",
        name = "Test Product",
        slug = "test-product",
        description = null,
        status = "ACTIVE",
        priceAmount = price,
        currency = "BRL",
        inventoryQuantity = 0,
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    )

    private var repoReturn: Product? = null

    private val fakeRepo = object : ProductRepositoryPort {
        override fun findByCategoryId(categoryId: Long, page: Int, size: Int): ProductPage =
            throw UnsupportedOperationException()

        override fun findByName(name: String, page: Int, size: Int): ProductPage =
            throw UnsupportedOperationException()

        override fun save(command: CreateProductCommand): Product =
            throw UnsupportedOperationException()

        override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? = repoReturn
    }

    private val useCase = UpdateProductPriceUseCase(fakeRepo)

    @Test
    fun `returns updated product when price is valid and product exists`() {
        repoReturn = aProduct(BigDecimal("99.90"))
        val result = useCase.execute(UpdatePriceCommand(1L, BigDecimal("99.90")))
        assertEquals(0, BigDecimal("99.90").compareTo(result.priceAmount))
    }

    @Test
    fun `throws ProductValidationException when priceAmount is zero`() {
        assertFailsWith<ProductValidationException> {
            useCase.execute(UpdatePriceCommand(1L, BigDecimal.ZERO))
        }
    }

    @Test
    fun `throws ProductValidationException when priceAmount is negative`() {
        assertFailsWith<ProductValidationException> {
            useCase.execute(UpdatePriceCommand(1L, BigDecimal("-1.00")))
        }
    }

    @Test
    fun `throws ProductNotFoundException when product does not exist`() {
        repoReturn = null
        assertFailsWith<ProductNotFoundException> {
            useCase.execute(UpdatePriceCommand(1L, BigDecimal("99.90")))
        }
    }
}
```

- [x] **Step 2: Run tests — expect compile failure (class does not exist yet)**

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test 2>&1 | grep -E "error:|FAILED|UpdateProductPriceUseCase"
```

Expected: compile error — `Unresolved reference: UpdateProductPriceUseCase`

- [x] **Step 3: Implement `UpdateProductPriceUseCase.kt`**

```kotlin
package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import java.math.BigDecimal
import org.springframework.stereotype.Service

@Service
class UpdateProductPriceUseCase(
    private val productRepository: ProductRepositoryPort,
) {

    fun execute(command: UpdatePriceCommand): Product {
        if (command.priceAmount <= BigDecimal.ZERO) {
            throw ProductValidationException("priceAmount must be greater than zero.")
        }
        return productRepository.updatePrice(command.id, command.priceAmount)
            ?: throw ProductNotFoundException("Product ${command.id} not found.")
    }
}
```

- [x] **Step 4: Run tests — expect 4 passing**

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test 2>&1 | grep -E "tests|PASSED|FAILED|UpdateProductPrice"
```

Expected: `UpdateProductPriceUseCaseTest > 4 tests` all PASSED. Build may still fail on `ProductRepository` — that is expected and will be fixed in Task 3.

- [x] **Step 5: Commit**

```bash
git add src/test/kotlin/com/nexus/shopping/product/UpdateProductPriceUseCaseTest.kt
git add src/main/kotlin/com/nexus/shopping/product/application/usecase/UpdateProductPriceUseCase.kt
git commit -m "feat: implement UpdateProductPriceUseCase with validation"
```

---

## Task 3: JDBC adapter

**Files:**
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jdbc/ProductRepository.kt`

**Interfaces:**
- Consumes: `ProductRepositoryPort.updatePrice(id: Long, priceAmount: BigDecimal): Product?`
- Produces: concrete implementation; returns `null` when no row matches `id`

- [x] **Step 1: Add `updatePrice` to `ProductRepository`**

Add this method after the existing `findByName` and before the private helpers. The existing private `toProduct()` extension is reused:

```kotlin
override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? {
    val updatedRows = jdbcTemplate.update(
        "UPDATE products SET price_amount = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        priceAmount,
        id,
    )

    if (updatedRows == 0) {
        return null
    }

    return jdbcTemplate.queryForObject("SELECT * FROM products WHERE id = ?", { rs, _ -> rs.toProduct() }, id)
}
```

Also add `import java.math.BigDecimal` to the file imports.

- [x] **Step 2: Run build — expect BUILD SUCCESSFUL**

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Expected: `BUILD SUCCESSFUL` — all tests pass, including the 4 new use case tests.

- [x] **Step 3: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jdbc/ProductRepository.kt
git commit -m "feat: implement updatePrice in ProductRepository"
```

---

## Task 4: HTTP adapter and controller tests

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/UpdatePriceRequest.kt`
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductController.kt`
- Modify: `src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt`

**Interfaces:**
- Consumes: `UpdateProductPriceUseCase.execute(UpdatePriceCommand): Product`, `ProductValidationException`, `ProductNotFoundException`
- Produces: `PATCH /products/{id}` → `200 Product` | `400` | `404`

- [x] **Step 1: Create `UpdatePriceRequest.kt`**

```kotlin
package com.nexus.shopping.product.adapter.inbound.http

import com.nexus.shopping.product.application.usecase.UpdatePriceCommand
import java.math.BigDecimal

data class UpdatePriceRequest(
    val priceAmount: BigDecimal,
) {
    fun toCommand(id: Long) = UpdatePriceCommand(id = id, priceAmount = priceAmount)
}
```

- [x] **Step 2: Add `PATCH /products/{id}` to `ProductController`**

The controller already has GET and POST. Add `UpdateProductPriceUseCase` as a constructor parameter and add the PATCH endpoint. Replace the entire file:

```kotlin
package com.nexus.shopping.product.adapter.inbound.http

import com.nexus.shopping.product.application.usecase.ProductCreateUseCase
import com.nexus.shopping.product.application.usecase.ProductNotFoundException
import com.nexus.shopping.product.application.usecase.ProductSearchUseCase
import com.nexus.shopping.product.application.usecase.ProductValidationException
import com.nexus.shopping.product.application.usecase.UpdateProductPriceUseCase
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/products")
class ProductController(
    private val productSearchUseCase: ProductSearchUseCase,
    private val productCreateUseCase: ProductCreateUseCase,
    private val updateProductPriceUseCase: UpdateProductPriceUseCase,
) {

    @GetMapping
    fun search(
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) name: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): ProductPage {
        try {
            return productSearchUseCase.search(categoryId, name, page, size)
        } catch (e: ProductValidationException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateProductRequest): Product {
        try {
            return productCreateUseCase.create(request.toCommand())
        } catch (e: ProductValidationException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        }
    }

    @PatchMapping("/{id}")
    fun updatePrice(
        @PathVariable id: Long,
        @RequestBody request: UpdatePriceRequest,
    ): Product {
        try {
            return updateProductPriceUseCase.execute(request.toCommand(id))
        } catch (e: ProductValidationException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        } catch (e: ProductNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
        }
    }
}
```

- [x] **Step 3: Add PATCH tests to `ProductControllerTest`**

Add a `patch` helper method and three new tests to the existing `ProductControllerTest` class. The seed has 3 products (id 1, 2, 3 via `productSeedCount=3`). Replace the entire file:

```kotlin
package com.nexus.shopping

import com.fasterxml.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:nexus_shopping_controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class ProductControllerTest {

    @Autowired
    private lateinit var environment: Environment

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    private fun post(port: String, body: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/products"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun patch(port: String, id: Long, body: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/products/$id"))
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    @Test
    fun `POST products returns 201 with created product`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body = """
            {
              "brandId": 1,
              "categoryId": 1,
              "sku": "SKU-CTRL-001",
              "name": "Controller Test Product",
              "slug": "controller-test-product",
              "priceAmount": 49.90
            }
        """.trimIndent()

        val response = post(port, body)

        assertEquals(201, response.statusCode())
        val product = mapper.readTree(response.body())
        assertNotNull(product["id"].asLong().takeIf { it > 0 }, "Expected a generated id > 0")
        assertEquals("SKU-CTRL-001", product["sku"].asText())
        assertEquals("Controller Test Product", product["name"].asText())
        assertEquals("ACTIVE", product["status"].asText())
        assertEquals("BRL", product["currency"].asText())
        assertEquals(0, product["inventoryQuantity"].asInt())
    }

    @Test
    fun `POST products with blank sku returns 400`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body = """
            {
              "brandId": 1,
              "categoryId": 1,
              "sku": "",
              "name": "Some Product",
              "slug": "some-product",
              "priceAmount": 10.00
            }
        """.trimIndent()

        val response = post(port, body)

        assertEquals(400, response.statusCode())
    }

    @Test
    fun `POST products with negative price returns 400`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body = """
            {
              "brandId": 1,
              "categoryId": 1,
              "sku": "SKU-NEG",
              "name": "Negative Price",
              "slug": "negative-price",
              "priceAmount": -1.00
            }
        """.trimIndent()

        val response = post(port, body)

        assertEquals(400, response.statusCode())
    }

    @Test
    fun `POST products with invalid status returns 400`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body = """
            {
              "brandId": 1,
              "categoryId": 1,
              "sku": "SKU-STATUS",
              "name": "Status Product",
              "slug": "status-product",
              "priceAmount": 10.00,
              "status": "DELETED"
            }
        """.trimIndent()

        val response = post(port, body)

        assertEquals(400, response.statusCode())
    }

    @Test
    fun `PATCH products updates price and returns 200 with full product`() {
        val port = environment.getRequiredProperty("local.server.port")
        val response = patch(port, 1L, """{"priceAmount": 99.90}""")

        assertEquals(200, response.statusCode())
        val product = mapper.readTree(response.body())
        assertEquals(1L, product["id"].asLong())
        assertEquals(0, BigDecimal("99.90").compareTo(BigDecimal(product["priceAmount"].asText())))
    }

    @Test
    fun `PATCH products with priceAmount zero returns 400`() {
        val port = environment.getRequiredProperty("local.server.port")
        val response = patch(port, 1L, """{"priceAmount": 0}""")
        assertEquals(400, response.statusCode())
    }

    @Test
    fun `PATCH products with non-existent id returns 404`() {
        val port = environment.getRequiredProperty("local.server.port")
        val response = patch(port, 9999999999L, """{"priceAmount": 99.90}""")
        assertEquals(404, response.statusCode())
    }
}
```

- [x] **Step 4: Run build — expect BUILD SUCCESSFUL**

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Expected: `BUILD SUCCESSFUL` — all tests pass, including the 3 new controller tests.

- [x] **Step 5: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/UpdatePriceRequest.kt
git add src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductController.kt
git add src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt
git commit -m "feat: add PATCH /products/{id} endpoint to update product price"
```
