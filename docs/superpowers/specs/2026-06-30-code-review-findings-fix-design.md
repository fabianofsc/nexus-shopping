# Correcao dos Findings do Code Review

**Status:** Pendente de implementacao
**Data:** 2026-06-30
**Branch alvo:** worktree isolada -> hexagonal-architecture -> main

---

## Contexto

Apos a execucao de `/code-review high` sobre os PRs #3 (update-product-price) e #4
(problem-details-error-handling), oito findings foram confirmados ou classificados como
plausiveis. Esta spec cobre a correcao de todos eles, agrupados por arquivo afetado.

---

## Findings corrigidos

### Alta severidade

**G5 — Race condition em `updatePrice` (ProductRepository.kt:76)**

UPDATE e SELECT executam em transacoes auto-commit separadas. Entre as duas operacoes,
uma escrita concorrente pode alterar o preco; o SELECT retorna o valor errado sem sinalizar
erro ao chamador.

### Media severidade

**B4 — `handleMessageNotReadable` descarta a excecao sem log (ProductExceptionHandler.kt:47)**

O metodo nao declara o parametro da excecao. Em producao, e impossivel distinguir erro do
cliente de regressao na serializacao sem a causa raiz nos logs.

**B1 — Tres caminhos de validacao de GET /products sem teste (ProductControllerTest.kt)**

Ausentes: `page<0`, `size>500`, busca sem nenhum parametro. Uma regressao nesses caminhos
passa silenciosa porque nenhum teste de controller os cobre.

**G6 — `assertNoInternalDetailsLeaked` falso-positiva em "constraint" (ProductControllerTest.kt:100)**

`assertFalse(body.contains("constraint", ignoreCase = true))` rejeita qualquer resposta
que contenha a palavra, incluindo mensagens de erro legitimas de negocio.

### Baixa severidade

**G3 — `handleResponseStatus` e codigo morto (ProductExceptionHandler.kt:82)**

Nada no codigo atual lanca `ResponseStatusException`. O handler silencia violacoes futuras
de design ao absorver a excecao sem sinal ao desenvolvedor.

**G4 — `ProductNotFoundException` no pacote errado (ProductNotFoundException.kt:1)**

"Produto nao existe" e um fato de dominio, como `Product` e `ProductPage`. Vive em
`application/usecase` quando deveria estar em `domain/`.

**B2 — Blank reason em `handleResponseStatus` (ProductExceptionHandler.kt:98)**

`exception.reason ?: statusCode.toString()` nao trata string vazia: reason `""` passa como
detail em branco, quebrando o contrato RFC 7807. Resolvido pela remocao de G3.

**E1 — Import alias `ProblemDetail as ExceptionDetail` (ProductExceptionHandler.kt:11)**

Renomeia tipo bem estabelecido do Spring para algo que nao existe fora do arquivo.

---

## Arquitetura

Sem novos arquivos. Sem novas dependencias. Todas as mudancas sao cirurgicas nos arquivos
existentes.

```
product/
  domain/
    ProductNotFoundException.kt          # movido de application/usecase
  application/
    usecase/
      ProductValidationException.kt      # permanece (regra de orquestracao)
      UpdateProductPriceUseCase.kt       # atualiza import
  adapter/
    inbound/http/
      ProductExceptionHandler.kt         # logging + remocao dead code + alias
    outbound/jdbc/
      ProductRepository.kt               # @Transactional em updatePrice
src/test/
  ProductControllerTest.kt               # 3 testes novos + remocao check generico
```

---

## Mudancas por arquivo

### `product/domain/ProductNotFoundException.kt` (movido)

Mesmo conteudo, novo pacote:

```kotlin
package com.nexus.shopping.product.domain

class ProductNotFoundException(message: String) : RuntimeException(message)
```

### `product/application/usecase/UpdateProductPriceUseCase.kt`

Atualizar import:

```kotlin
import com.nexus.shopping.product.domain.ProductNotFoundException
```

### `product/adapter/inbound/http/ProductExceptionHandler.kt`

Cinco mudancas:

1. Atualizar import de `ProductNotFoundException` para o novo pacote:
   ```kotlin
   import com.nexus.shopping.product.domain.ProductNotFoundException
   ```

2. Remover alias: `import org.springframework.http.ProblemDetail` (sem `as ExceptionDetail`);
   substituir todas as ocorrencias de `ExceptionDetail` por `ProblemDetail` no arquivo.

3. Adicionar parametro e log em `handleMessageNotReadable`:
   ```kotlin
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
   ```

4. Remover metodo `handleResponseStatus` inteiro.

5. Remover import de `ResponseStatusException`.

6. Renomear funcoes privadas `exceptionDetailResponse` e `buildDetail` para
   `problemDetailResponse` e `buildProblemDetail` para eliminar a palavra "exception"
   que vinha do alias.

### `product/adapter/outbound/jdbc/ProductRepository.kt`

Adicionar `@Transactional` em `updatePrice`:

```kotlin
import org.springframework.transaction.annotation.Transactional

@Transactional
override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? {
    val updatedRows = jdbcTemplate.update(
        "UPDATE products SET price_amount = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        priceAmount,
        id,
    )
    if (updatedRows == 0) return null
    return jdbcTemplate.queryForObject(
        "SELECT * FROM products WHERE id = ?",
        { rs, _ -> rs.toProduct() },
        id,
    )
}
```

### `src/test/kotlin/com/nexus/shopping/product/ProductControllerTest.kt`

**Remover** linha 100:
```kotlin
assertFalse(body.contains("constraint", ignoreCase = true))
```

**Adicionar** tres testes (seguindo o padrao existente com `assertExceptionDetail`):

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
    )
}
```

Os tres testes nao verificam `expectedDetail` porque as mensagens exatas pertencem ao
use case e podem evoluir sem quebrar o contrato HTTP.

---

## Validacao

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Nenhuma nova migration. Nenhuma mudanca de contrato HTTP externo. Todos os testes
existentes devem continuar passando.

---

## Criterios de aceite

- `updatePrice` esta anotado com `@Transactional`.
- `ProductNotFoundException` esta em `com.nexus.shopping.product.domain`.
- `handleMessageNotReadable` declara o parametro da excecao e emite `logger.warn`.
- `handleResponseStatus` nao existe mais no arquivo.
- `ProblemDetail` e usado diretamente (sem alias).
- `assertNoInternalDetailsLeaked` nao contem o check generico de `"constraint"`.
- Tres novos testes de controller cobrem `page<0`, `size>500` e busca sem parametro.
- Build passa sem erros.
