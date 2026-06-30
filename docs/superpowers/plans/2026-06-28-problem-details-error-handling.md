# Problem Details Error Handling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Status:** Implementado e mergeado em main — PR #4 (codex/problem-details-error-handling)

**Goal:** Add a global Spring error handler that returns RFC 7807 Problem Details for validation, not-found, framework 4xx, and unhandled 5xx errors.

**Architecture:** Keep error translation in the inbound HTTP adapter. Application use cases continue throwing typed application exceptions (`ProductValidationException`, `ProductNotFoundException`), while `ProductExceptionHandler` translates them to HTTP Problem Details. `ProductController` becomes a thin adapter with no local `try/catch` error mapping.

**Tech Stack:** Kotlin, Java 21, Spring Boot 4, Spring Web `ProblemDetail`, `@RestControllerAdvice`, `java.net.http.HttpClient` controller tests, H2 test database.

---

## Global Constraints

- All shell commands must be prefixed with `rtk`.
- Use the Gradle wrapper command:

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

- Keep validation in application use cases.
- Do not add Bean Validation annotations for this task.
- Do not create a custom error DTO.
- Do not add project-specific Problem Details `type` URLs.
- Keep `type` as Spring's default `about:blank`.
- Do not expose stack traces, exception class names, SQL details, datasource details, file paths, or framework internals in 5xx response bodies.
- Keep documentation and new source text ASCII where practical.

---

## File Map

| Action | File |
|---|---|
| Create | `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductExceptionHandler.kt` |
| Modify | `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductController.kt` |
| Modify | `src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt` |

---

## Task 1: Add failing HTTP contract tests

**Files:**
- Modify: `src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt`

**Purpose:** Lock the RFC 7807 response contract before changing production code. These tests should fail while `ProductController` still uses local `ResponseStatusException` mapping and there is no global advice.

- [ ] **Step 1: Replace `ProductControllerTest.kt` with contract-aware tests**

Use this full file content:

```kotlin
package com.nexus.shopping

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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

    private fun get(port: String, query: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/products$query"))
            .GET()
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun post(
        port: String,
        body: String,
        contentType: String = "application/json",
    ): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/products"))
            .header("Content-Type", contentType)
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

    private fun delete(port: String, id: Long): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/products/$id"))
            .DELETE()
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun assertProblem(
        response: HttpResponse<String>,
        expectedStatus: Int,
        expectedTitle: String,
        expectedInstance: String,
        expectedDetail: String? = null,
    ): JsonNode {
        assertEquals(expectedStatus, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/problem+json"))
        val problem = mapper.readTree(response.body())
        assertEquals("about:blank", problem["type"].asText())
        assertEquals(expectedTitle, problem["title"].asText())
        assertEquals(expectedStatus, problem["status"].asInt())
        assertEquals(expectedInstance, problem["instance"].asText())
        if (expectedDetail != null) {
            assertEquals(expectedDetail, problem["detail"].asText())
        }
        return problem
    }

    private fun assertNoInternalDetailsLeaked(body: String) {
        assertFalse(body.contains("DataIntegrityViolationException"))
        assertFalse(body.contains("JdbcSQLIntegrityConstraintViolationException"))
        assertFalse(body.contains("Referential integrity"))
        assertFalse(body.contains("constraint", ignoreCase = true))
        assertFalse(body.contains("org.springframework"))
        assertFalse(body.contains("java.sql"))
        assertFalse(body.contains("\tat "))
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
    fun `GET products with both categoryId and name returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "?categoryId=1&name=Product&page=0&size=50")

        assertProblem(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "Use either categoryId or name, not both.",
        )
    }

    @Test
    fun `POST products with blank sku returns 400 problem details`() {
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

        assertProblem(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "sku must not be blank.",
        )
    }

    @Test
    fun `POST products with negative price returns 400 problem details`() {
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

        assertProblem(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "priceAmount must be >= 0.",
        )
    }

    @Test
    fun `POST products with invalid status returns 400 problem details`() {
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

        assertProblem(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "status must be one of: ACTIVE, INACTIVE, ARCHIVED.",
        )
    }

    @Test
    fun `POST products with malformed JSON returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = post(port, """{"brandId":""")

        assertProblem(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products",
            expectedDetail = "Malformed request body.",
        )
    }

    @Test
    fun `POST products with missing foreign key returns 500 generic problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body = """
            {
              "brandId": 999999999,
              "categoryId": 1,
              "sku": "SKU-FK-FAIL",
              "name": "Foreign Key Failure",
              "slug": "foreign-key-failure",
              "priceAmount": 10.00
            }
        """.trimIndent()

        val response = post(port, body)

        assertProblem(
            response = response,
            expectedStatus = 500,
            expectedTitle = "Internal Server Error",
            expectedInstance = "/products",
            expectedDetail = "Unexpected server error.",
        )
        assertNoInternalDetailsLeaked(response.body())
    }

    @Test
    fun `POST products with unsupported media type returns 415 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = post(port, "plain text", contentType = "text/plain")

        assertProblem(
            response = response,
            expectedStatus = 415,
            expectedTitle = "Unsupported Media Type",
            expectedInstance = "/products",
            expectedDetail = "Content type is not supported.",
        )
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
    fun `PATCH products with priceAmount zero returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val response = patch(port, 1L, """{"priceAmount": 0}""")

        assertProblem(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/products/1",
            expectedDetail = "priceAmount must be greater than zero.",
        )
    }

    @Test
    fun `PATCH products with non-existent id returns 404 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val response = patch(port, 9999999999L, """{"priceAmount": 99.90}""")

        assertProblem(
            response = response,
            expectedStatus = 404,
            expectedTitle = "Not Found",
            expectedInstance = "/products/9999999999",
            expectedDetail = "Product 9999999999 not found.",
        )
    }

    @Test
    fun `DELETE products returns 405 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val response = delete(port, 1L)

        assertProblem(
            response = response,
            expectedStatus = 405,
            expectedTitle = "Method Not Allowed",
            expectedInstance = "/products/1",
            expectedDetail = "Request method is not supported.",
        )
    }
}
```

- [ ] **Step 2: Run controller tests and verify failure**

Run:

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests com.nexus.shopping.ProductControllerTest
```

Expected: FAIL. The new Problem Details body and content-type assertions fail because the global advice does not exist yet and the controller still maps application errors through `ResponseStatusException`.

---

## Task 2: Add the global ProductExceptionHandler

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductExceptionHandler.kt`

**Purpose:** Centralize HTTP error translation in the inbound adapter. This implements RFC 7807 Problem Details for application exceptions, selected framework 4xx exceptions, and unhandled exceptions.

- [ ] **Step 1: Create `ProductExceptionHandler.kt`**

Use this full file content:

```kotlin
package com.nexus.shopping.product.adapter.inbound.http

import com.nexus.shopping.product.application.usecase.ProductNotFoundException
import com.nexus.shopping.product.application.usecase.ProductValidationException
import jakarta.servlet.http.HttpServletRequest
import java.net.URI
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ProductExceptionHandler {

    @ExceptionHandler(ProductValidationException::class)
    fun handleValidation(
        exception: ProductValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.BAD_REQUEST,
            detail = exception.message ?: "Validation failed.",
            request = request,
        )

    @ExceptionHandler(ProductNotFoundException::class)
    fun handleNotFound(
        exception: ProductNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.NOT_FOUND,
            detail = exception.message ?: "Resource not found.",
            request = request,
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.BAD_REQUEST,
            detail = "Malformed request body.",
            request = request,
        )

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        exception: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.METHOD_NOT_ALLOWED,
            detail = "Request method is not supported.",
            request = request,
        )

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(
        exception: HttpMediaTypeNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            detail = "Content type is not supported.",
            request = request,
        )

    @ExceptionHandler(Exception::class)
    fun handleUnhandled(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        logger.error("Unhandled exception while processing request", exception)
        return problem(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            detail = "Unexpected server error.",
            request = request,
        )
    }

    private fun problem(
        status: HttpStatus,
        detail: String,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.title = status.reasonPhrase
        problem.instance = URI.create(request.requestURI)
        return ResponseEntity.status(status).body(problem)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ProductExceptionHandler::class.java)
    }
}
```

- [ ] **Step 2: Run controller tests and verify remaining failures**

Run:

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests com.nexus.shopping.ProductControllerTest
```

Expected: FAIL. The tests for application exceptions still fail because `ProductController` catches `ProductValidationException` and `ProductNotFoundException` and rethrows `ResponseStatusException`. The malformed JSON, DELETE 405, unsupported media type 415, and missing foreign key 500 tests should now pass.

---

## Task 3: Remove local exception mapping from ProductController

**Files:**
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductController.kt`

**Purpose:** Let typed application exceptions propagate to `ProductExceptionHandler`. The controller should translate HTTP input to use-case calls and return successful responses only.

- [ ] **Step 1: Replace `ProductController.kt` with a thin controller**

Use this full file content:

```kotlin
package com.nexus.shopping.product.adapter.inbound.http

import com.nexus.shopping.product.application.usecase.ProductCreateUseCase
import com.nexus.shopping.product.application.usecase.ProductSearchUseCase
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
    ): ProductPage =
        productSearchUseCase.search(categoryId, name, page, size)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateProductRequest): Product =
        productCreateUseCase.create(request.toCommand())

    @PatchMapping("/{id}")
    fun updatePrice(
        @PathVariable id: Long,
        @RequestBody request: UpdatePriceRequest,
    ): Product =
        updateProductPriceUseCase.execute(request.toCommand(id))
}
```

- [ ] **Step 2: Run controller tests and verify they pass**

Run:

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests com.nexus.shopping.ProductControllerTest
```

Expected: PASS for `ProductControllerTest`.

- [ ] **Step 3: Commit the HTTP error-handling slice**

Run:

```bash
git add src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductExceptionHandler.kt src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductController.kt src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt
git commit -m "feat: standardize product error responses"
```

Expected: commit succeeds.

---

## Task 4: Full verification

**Files:**
- No source changes expected.

**Purpose:** Verify the whole project still builds and all existing search/create/update behavior remains intact.

- [ ] **Step 1: Run the full build**

Run:

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Check git status**

Run:

```bash
git status --short
```

Expected: clean output.

- [ ] **Step 3: Record verification result in the final response**

Include:

```text
Verified with:
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Also include the commit hash from Task 3.
