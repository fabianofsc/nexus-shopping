# Update Product Price — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `PATCH /products/{id}` that updates only `priceAmount`, returning the full updated product or 400/404 on error.

**Architecture:** Hexagonal — new `UpdateProductPriceUseCase` orchestrates validation and calls `ProductRepositoryPort.updatePrice`, which executes a single `UPDATE … RETURNING *`. Errors are typed exceptions caught by the controller (same pattern as `ProductSearchUseCase`).

**Tech Stack:** Kotlin, Spring Boot 4, JdbcTemplate, PostgreSQL / H2 (tests)

## Global Constraints

- All shell commands must be prefixed with `rtk`
- Use Gradle wrapper: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build`
- No JPA/ORM — JDBC only
- Test style: `kotlin.test` (`assertFailsWith`, `assertEquals`), hand-rolled fakes — no Mockito
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
| Fix | `src/test/kotlin/com/nexus/shopping/product/ProductSearchUseCaseTest.kt` |

---

## Task 1: Foundation types, port contract, and compilation fix

> **Context:** `ProductSearchUseCaseTest.kt` currently references `CreateProductCommand` (not yet implemented) and an `override fun save(...)` that does not exist in `ProductRepositoryPort`. This prevents compilation. This task creates the foundation types for our feature and fixes the broken test file.

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/product/application/usecase/UpdatePriceCommand.kt`
- Create: `src/main/kotlin/com/nexus/shopping/product/application/usecase/ProductNotFoundException.kt`
- Modify: `src/main/kotlin/com/nexus/shopping/product/application/port/outbound/ProductRepositoryPort.kt`
- Fix: `src/test/kotlin/com/nexus/shopping/product/ProductSearchUseCaseTest.kt`

**Interfaces:**
- Produces: `UpdatePriceCommand(id: Long, priceAmount: BigDecimal)`, `ProductNotFoundException`, `ProductRepositoryPort.updatePrice(id: Long, priceAmount: BigDecimal): Product?`

- [ ] **Step 1: Create `UpdatePriceCommand.kt`**

```kotlin
package com.nexus.shopping.product.application.usecase

import java.math.BigDecimal

data class UpdatePriceCommand(
    val id: Long,
    val priceAmount: BigDecimal,
)
```

- [ ] **Step 2: Create `ProductNotFoundException.kt`**

```kotlin
package com.nexus.shopping.product.application.usecase

class ProductNotFoundException(message: String) : RuntimeException(message)
```

- [ ] **Step 3: Add `updatePrice` to `ProductRepositoryPort`**

Replace the entire file content:

```kotlin
package com.nexus.shopping.product.application.port.outbound

import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductPage
import java.math.BigDecimal

interface ProductRepositoryPort {
    fun findByCategoryId(categoryId: Long, page: Int, size: Int): ProductPage
    fun findByName(name: String, page: Int, size: Int): ProductPage
    fun updatePrice(id: Long, priceAmount: BigDecimal): Product?
}
```

- [ ] **Step 4: Fix `ProductSearchUseCaseTest.kt`**

The existing test has a broken `save` override and a `CreateProductCommand` import that do not belong yet. Replace the file with the corrected version that stubs `updatePrice` instead:

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

        override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? =
            throw UnsupportedOperationException()
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

- [ ] **Step 5: Run build — expect all tests to pass**

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Expected: `BUILD SUCCESSFUL` — `ProductRepository` still compiles because Kotlin will flag the missing `updatePrice` implementation as an error, so check output. If it fails because `ProductRepository` doesn't implement `updatePrice` yet, that is expected and fine — proceed to Task 2; the compile error will be fixed in Task 3.

> **Note:** If the build fails only on `ProductRepository` not implementing `updatePrice`, continue. If it fails for any other reason, fix it before continuing.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/product/application/usecase/UpdatePriceCommand.kt
git add src/main/kotlin/com/nexus/shopping/product/application/usecase/ProductNotFoundException.kt
git add src/main/kotlin/com/nexus/shopping/product/application/port/outbound/ProductRepositoryPort.kt
git add src/test/kotlin/com/nexus/shopping/product/ProductSearchUseCaseTest.kt
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

- [ ] **Step 1: Write the failing test**

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

- [ ] **Step 2: Run tests — expect compile failure (class does not exist yet)**

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test 2>&1 | grep -E "error:|FAILED|UpdateProductPriceUseCase"
```

Expected: compile error — `Unresolved reference: UpdateProductPriceUseCase`

- [ ] **Step 3: Implement `UpdateProductPriceUseCase.kt`**

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

- [ ] **Step 4: Run tests — expect 4 passing**

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test 2>&1 | grep -E "tests|PASSED|FAILED|UpdateProductPrice"
```

Expected: `UpdateProductPriceUseCaseTest > 4 tests` all PASSED. Build may still fail due to `ProductRepository` not implementing `updatePrice` — that is fine; the test task itself should pass.

- [ ] **Step 5: Commit**

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

- [ ] **Step 1: Add `updatePrice` to `ProductRepository`**

Add this method to the `ProductRepository` class (after `findByName`, before the private helpers). The existing `toProduct()` extension is reused:

```kotlin
override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? =
    jdbcTemplate.query(
        "UPDATE products SET price_amount = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? RETURNING *",
        { resultSet, _ -> resultSet.toProduct() },
        priceAmount,
        id,
    ).firstOrNull()
```

Also add `import java.math.BigDecimal` to the file imports if not already present.

- [ ] **Step 2: Run build — expect BUILD SUCCESSFUL**

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Expected: `BUILD SUCCESSFUL` — all tests pass, including the 4 new use case tests.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jdbc/ProductRepository.kt
git commit -m "feat: implement updatePrice in ProductRepository via UPDATE RETURNING"
```

---

## Task 4: HTTP adapter

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/UpdatePriceRequest.kt`
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductController.kt`

**Interfaces:**
- Consumes: `UpdateProductPriceUseCase.execute(UpdatePriceCommand): Product`, `ProductValidationException`, `ProductNotFoundException`
- Produces: `PATCH /products/{id}` → `200 Product` | `400` | `404`

- [ ] **Step 1: Create `UpdatePriceRequest.kt`**

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

- [ ] **Step 2: Add `PATCH /products/{id}` to `ProductController`**

Add `UpdateProductPriceUseCase` as a constructor parameter and add the new endpoint. Full updated file:

```kotlin
package com.nexus.shopping.product.adapter.inbound.http

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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/products")
class ProductController(
    private val productSearchUseCase: ProductSearchUseCase,
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

- [ ] **Step 3: Run build — expect BUILD SUCCESSFUL**

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Smoke-test the endpoint manually (optional but recommended)**

Start the stack and send a PATCH request:

```bash
rtk docker compose up -d postgres
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew bootRun &
# wait for health
curl -s http://localhost:8080/actuator/health

# update price of product id=1
curl -s -X PATCH http://localhost:8080/products/1 \
  -H "Content-Type: application/json" \
  -d '{"priceAmount": 99.90}' | jq .

# verify 404
curl -s -X PATCH http://localhost:8080/products/9999999999 \
  -H "Content-Type: application/json" \
  -d '{"priceAmount": 99.90}' -w "\nHTTP %{http_code}\n"

# verify 400
curl -s -X PATCH http://localhost:8080/products/1 \
  -H "Content-Type: application/json" \
  -d '{"priceAmount": 0}' -w "\nHTTP %{http_code}\n"
```

Expected:
- First call: `200` with full product JSON, `priceAmount` = `99.90`
- Second call: `HTTP 404`
- Third call: `HTTP 400`

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/UpdatePriceRequest.kt
git add src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductController.kt
git commit -m "feat: add PATCH /products/{id} endpoint to update product price"
```
