# Plano: Reorganizacao de pacotes para crescimento hexagonal

**Spec:** `docs/superpowers/specs/2026-06-30-package-structure-design.md`
**Data:** 2026-06-30
**Status:** Pendente de implementacao

---

## Objetivo

Reorganizar pacotes para preparar o projeto para novos dominios de ecommerce, mantendo a
arquitetura hexagonal e sem alterar comportamento externo da API.

---

## Tarefas

### 1. Criar excecoes base em `platform`

Criar pacote:

```text
src/main/kotlin/com/nexus/shopping/platform/application/exception/
```

Criar:

- `ApplicationException`
- `ValidationException`
- `NotFoundException`

As classes nao devem importar Spring, HTTP, JDBC ou qualquer framework.

### 2. Mover excecoes especificas de produto

Criar pacote:

```text
src/main/kotlin/com/nexus/shopping/product/application/exception/
```

Mover:

- `ProductValidationException`
- `ProductNotFoundException`

Atualizar hierarquia:

- `ProductValidationException` estende `ValidationException`
- `ProductNotFoundException` estende `NotFoundException`

Atualizar imports em use cases e testes.

### 3. Separar commands dos use cases

Criar pacote:

```text
src/main/kotlin/com/nexus/shopping/product/application/command/
```

Mover:

- `CreateProductCommand`
- `UpdatePriceCommand`

Atualizar imports em:

- use cases
- porta outbound
- adapter JDBC
- DTOs HTTP
- testes

### 4. Generalizar tratamento HTTP de erros

Mover o handler para:

```text
src/main/kotlin/com/nexus/shopping/platform/adapter/inbound/http/ApiExceptionHandler.kt
```

Renomear `ProductExceptionHandler` para `ApiExceptionHandler`.

Mapear:

- `ValidationException` -> 400
- `NotFoundException` -> 404
- `HttpMessageNotReadableException` -> 400
- `HttpRequestMethodNotSupportedException` -> 405 com header `Allow`
- `HttpMediaTypeNotSupportedException` -> 415
- outros `ErrorResponse` 4xx -> status original
- `MethodArgumentTypeMismatchException` -> 400
- excecoes inesperadas -> 500 generico

O handler nao deve importar classes de produto.

### 5. Criar DTOs HTTP de produto

Criar pacote:

```text
src/main/kotlin/com/nexus/shopping/product/adapter/inbound/http/dto/
```

Mover:

- `CreateProductRequest`
- `UpdatePriceRequest`

Criar responses:

- `ProductResponse`
- `ProductPageResponse`

O shape JSON deve permanecer compativel com o retorno atual de `Product` e `ProductPage`.

### 6. Colocalizar extension functions de conversao nos DTOs HTTP

Criar funcoes de conversao dentro dos proprios arquivos de request/response em
`product/adapter/inbound/http/dto/`:

- request -> command
- `Product` -> `ProductResponse`
- `ProductPage` -> `ProductPageResponse`

Manter as conversoes no adapter HTTP, nao nos use cases nem no dominio. Nao criar pacote
separado de `mapper` nesta etapa.

### 7. Atualizar controller

Atualizar `ProductController` para:

- receber DTOs HTTP do pacote `dto`
- converter requests para commands
- chamar use cases
- converter respostas de dominio para responses HTTP

O controller deve continuar sem regras de negocio.

### 8. Atualizar testes

Atualizar imports e expectativas nos testes existentes.

Garantir cobertura para:

- use cases lancando excecoes especificas que herdam das bases de `platform`
- `GET /products` retornando mesmo JSON de pagina atual
- `POST /products` retornando mesmo JSON de produto atual
- `PATCH /products/{id}` retornando mesmo JSON de produto atual
- Problem Details para 400, 404, 405, 415 e 500

### 9. Verificacao

Executar:

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

O build deve passar.

---

## Cuidados de implementacao

- Nao criar pacote `shared`.
- Nao mover regras de negocio para `platform`.
- Nao alterar migrations.
- Nao alterar mensagens de validacao existentes.
- Nao alterar o formato RFC 7807 das respostas de erro.
- Nao introduzir novas dependencias.
- Evitar refatoracoes fora do escopo da estrutura de pacotes.
