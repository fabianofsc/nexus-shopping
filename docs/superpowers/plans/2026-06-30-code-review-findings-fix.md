# Correcao dos Findings do Code Review — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corrigir os 8 findings do code review: race condition em updatePrice, logging ausente, 3 testes faltando, check generico de "constraint", dead code no handler, pacote errado de ProductNotFoundException e import alias.

**Architecture:** Mudancas cirurgicas em 5 arquivos existentes. Sem novas dependencias, sem novo schema, sem novos contratos HTTP. A unica mudanca estrutural e mover ProductNotFoundException de application/usecase para domain/.

**Tech Stack:** Kotlin, Spring Boot 4, JdbcTemplate, H2 (testes), Spring Transaction (@Transactional), kotlin.test.

## Global Constraints

- Todos os comandos shell prefixados com `rtk`
- Gradle Wrapper com GRADLE_USER_HOME local: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build`
- Manter compatibilidade H2 nos testes (sem RETURNING * em UPDATE)
- Sem novos arquivos alem dos indicados
- Commits em ingles, agrupados por contexto
- Worktree ja criada antes de comecar (usar superpowers:using-git-worktrees se necessario)

---

### Task 1: Mover ProductNotFoundException para domain/

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/product/domain/ProductNotFoundException.kt`
- Delete: `src/main/kotlin/com/nexus/shopping/product/application/usecase/ProductNotFoundException.kt`
- Modify: `src/main/kotlin/com/nexus/shopping/product/application/usecase/UpdateProductPriceUseCase.kt` (adicionar import explicito)
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductExceptionHandler.kt` (linha 3: atualizar import)

**Interfaces:**
- Produces: `com.nexus.shopping.product.domain.ProductNotFoundException` — mesmo contrato, novo pacote. Tasks 2 e 3 dependem deste novo caminho.

- [ ] **Step 1: Criar ProductNotFoundException no pacote domain/**

Conteudo de `src/main/kotlin/com/nexus/shopping/product/domain/ProductNotFoundException.kt`:

```kotlin
package com.nexus.shopping.product.domain

class ProductNotFoundException(message: String) : RuntimeException(message)
```

- [ ] **Step 2: Deletar arquivo antigo**

```bash
rm src/main/kotlin/com/nexus/shopping/product/application/usecase/ProductNotFoundException.kt
```

- [ ] **Step 3: Adicionar import em UpdateProductPriceUseCase.kt**

O arquivo usava `ProductNotFoundException` sem import porque estava no mesmo pacote. Substituir o conteudo completo do arquivo:

```kotlin
package com.nexus.shopping.product.application.usecase

import com.nexus.shopping.product.application.port.outbound.ProductRepositoryPort
import com.nexus.shopping.product.domain.Product
import com.nexus.shopping.product.domain.ProductNotFoundException
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

- [ ] **Step 4: Atualizar import em ProductExceptionHandler.kt**

Linha 3 atual:
```kotlin
import com.nexus.shopping.product.application.usecase.ProductNotFoundException
```

Substituir por:
```kotlin
import com.nexus.shopping.product.domain.ProductNotFoundException
```

- [ ] **Step 5: Verificar build**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Esperado: BUILD SUCCESSFUL. Se houver erro "Unresolved reference: ProductNotFoundException", algum import ficou faltando.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/product/domain/ProductNotFoundException.kt \
        src/main/kotlin/com/nexus/shopping/product/application/usecase/UpdateProductPriceUseCase.kt \
        src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductExceptionHandler.kt
git rm src/main/kotlin/com/nexus/shopping/product/application/usecase/ProductNotFoundException.kt
git commit -m "refactor: move ProductNotFoundException to domain layer"
```

---

### Task 2: Corrigir race condition com @Transactional em updatePrice

**Files:**
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jdbc/ProductRepository.kt`

**Interfaces:**
- Consumes: `ProductRepositoryPort.updatePrice(id: Long, priceAmount: BigDecimal): Product?`
- Produces: mesmo contrato, agora atomico via transacao Spring

- [ ] **Step 1: Adicionar import de @Transactional**

No bloco de imports de `ProductRepository.kt`, adicionar apos os imports existentes:

```kotlin
import org.springframework.transaction.annotation.Transactional
```

- [ ] **Step 2: Anotar updatePrice com @Transactional**

Localizar (linha 76):
```kotlin
override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? {
```

Substituir por:
```kotlin
@Transactional
override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? {
```

- [ ] **Step 3: Rodar build**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Esperado: BUILD SUCCESSFUL. Os testes PATCH existentes devem continuar passando.

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jdbc/ProductRepository.kt
git commit -m "fix: wrap updatePrice UPDATE+SELECT in @Transactional to eliminate race condition"
```

---

### Task 3: Corrigir ProductExceptionHandler

**Files:**
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductExceptionHandler.kt`

**Interfaces:**
- Sem mudancas de contrato HTTP externo. Todos os endpoints continuam respondendo com os mesmos status codes e formato Problem Details.

Cinco mudancas neste arquivo:
1. Import de ProductNotFoundException ja atualizado na Task 1 (verificar que esta correto)
2. Remover alias `ProblemDetail as ExceptionDetail`, usar `ProblemDetail` diretamente
3. Adicionar `exception: HttpMessageNotReadableException` como parametro + `logger.warn` em handleMessageNotReadable
4. Remover metodo `handleResponseStatus` e import de `ResponseStatusException`
5. Renomear funcoes privadas: `exceptionDetailResponse` -> `problemDetailResponse`, `buildDetail` -> `buildProblemDetail`

- [ ] **Step 1: Substituir o arquivo completo**

Escrever o seguinte conteudo em `src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductExceptionHandler.kt`:

```kotlin
package com.nexus.shopping.product.adapter.inbound.http

import com.nexus.shopping.product.domain.ProductNotFoundException
import com.nexus.shopping.product.application.usecase.ProductValidationException
import jakarta.servlet.http.HttpServletRequest
import java.net.URI
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponse
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ProductExceptionHandler {

    @ExceptionHandler(ProductValidationException::class)
    fun handleValidation(
        exception: ProductValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problemDetailResponse(
            status = HttpStatus.BAD_REQUEST,
            detail = exception.message ?: "Validation failed.",
            request = request,
        )

    @ExceptionHandler(ProductNotFoundException::class)
    fun handleNotFound(
        exception: ProductNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problemDetailResponse(
            status = HttpStatus.NOT_FOUND,
            detail = exception.message ?: "Resource not found.",
            request = request,
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        logger.warn("Malformed request body", exception)
        return problemDetailResponse(
            status = HttpStatus.BAD_REQUEST,
            detail = "Malformed request body.",
            request = request,
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        exception: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val detail = buildProblemDetail(
            status = HttpStatus.METHOD_NOT_ALLOWED,
            detail = "Request method is not supported.",
            request = request,
        )
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .allow(*exception.supportedHttpMethods.orEmpty().toTypedArray())
            .body(detail)
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problemDetailResponse(
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            detail = "Content type is not supported.",
            request = request,
        )

    @ExceptionHandler(Exception::class)
    fun handleUnhandled(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        if (exception is ErrorResponse && exception.statusCode.is4xxClientError) {
            return problemDetailResponse(
                status = exception.statusCode,
                detail = exception.body.detail?.takeIf { it.isNotBlank() } ?: "Invalid request.",
                request = request,
            )
        }

        if (exception is MethodArgumentTypeMismatchException) {
            return problemDetailResponse(
                status = HttpStatus.BAD_REQUEST,
                detail = "Invalid request.",
                request = request,
            )
        }

        logger.error("Unhandled exception while processing request", exception)
        return problemDetailResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            detail = "Unexpected server error.",
            request = request,
        )
    }

    private fun problemDetailResponse(
        status: HttpStatusCode,
        detail: String,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problemDetail = buildProblemDetail(status, detail, request)
        return ResponseEntity.status(status).body(problemDetail)
    }

    private fun buildProblemDetail(
        status: HttpStatusCode,
        detail: String,
        request: HttpServletRequest,
    ): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(status, detail)
        problemDetail.type = URI.create("about:blank")
        problemDetail.title = HttpStatus.resolve(status.value())?.reasonPhrase ?: status.toString()
        problemDetail.instance = URI.create(request.requestURI)
        return problemDetail
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ProductExceptionHandler::class.java)
    }
}
```

- [ ] **Step 2: Rodar build**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Esperado: BUILD SUCCESSFUL. Todos os testes de controller existentes devem passar incluindo malformed JSON (400), method not allowed (405), unsupported media type (415) e missing FK (500).

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/ProductExceptionHandler.kt
git commit -m "fix: add exception logging, remove dead code and ProblemDetail alias in ProductExceptionHandler"
```

---

### Task 4: Corrigir ProductControllerTest

**Files:**
- Modify: `src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt`

**Interfaces:**
- Consumes: metodos helper existentes `get(port, query): HttpResponse<String>` e `assertExceptionDetail(response, expectedStatus, expectedTitle, expectedInstance, expectedDetail?)`.
- As mensagens exatas de validacao vem de `ProductSearchUseCase`:
  - page < 0: `"Query parameter page must be greater than or equal to 0."`
  - size fora de 1..500: `"Query parameter size must be between 1 and 500."`
  - sem parametro: `"Query parameter categoryId or name is required."`

Duas mudancas:
1. Remover check generico de `"constraint"` em `assertNoInternalDetailsLeaked` (linha 100)
2. Adicionar 3 testes para caminhos de validacao de GET /products sem cobertura

Os novos testes cobrem comportamento ja implementado — passarao imediatamente.

- [ ] **Step 1: Remover check generico de "constraint"**

Localizar em `assertNoInternalDetailsLeaked` a linha:
```kotlin
assertFalse(body.contains("constraint", ignoreCase = true))
```

Remover essa linha. O metodo fica:

```kotlin
private fun assertNoInternalDetailsLeaked(body: String) {
    assertFalse(body.contains("DataIntegrityViolationException"))
    assertFalse(body.contains("JdbcSQLIntegrityConstraintViolationException"))
    assertFalse(body.contains("Referential integrity"))
    assertFalse(body.contains("org.springframework"))
    assertFalse(body.contains("java.sql"))
    assertFalse(body.contains("\tat "))
}
```

- [ ] **Step 2: Adicionar 3 testes de validacao de busca**

Adicionar os tres metodos a seguir antes do fechamento da classe (antes do ultimo `}`):

```kotlin
@Test
fun `GET products with page below zero returns 400 problem details`() {
    val port = environment.getRequiredProperty("local.server.port")

    val response = get(port, "?categoryId=1&page=-1&size=50")

    assertExceptionDetail(
        response = response,
        expectedStatus = 400,
        expectedTitle = "Bad Request",
        expectedInstance = "/products",
        expectedDetail = "Query parameter page must be greater than or equal to 0.",
    )
}

@Test
fun `GET products with size above limit returns 400 problem details`() {
    val port = environment.getRequiredProperty("local.server.port")

    val response = get(port, "?categoryId=1&page=0&size=999")

    assertExceptionDetail(
        response = response,
        expectedStatus = 400,
        expectedTitle = "Bad Request",
        expectedInstance = "/products",
        expectedDetail = "Query parameter size must be between 1 and 500.",
    )
}

@Test
fun `GET products with no search parameter returns 400 problem details`() {
    val port = environment.getRequiredProperty("local.server.port")

    val response = get(port, "?page=0&size=50")

    assertExceptionDetail(
        response = response,
        expectedStatus = 400,
        expectedTitle = "Bad Request",
        expectedInstance = "/products",
        expectedDetail = "Query parameter categoryId or name is required.",
    )
}
```

- [ ] **Step 3: Rodar build completo**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Esperado: BUILD SUCCESSFUL com 16 testes passando (eram 13 antes desta task).

- [ ] **Step 4: Commit**

```bash
git add src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt
git commit -m "test: add missing GET validation tests and remove overly broad constraint check"
```
