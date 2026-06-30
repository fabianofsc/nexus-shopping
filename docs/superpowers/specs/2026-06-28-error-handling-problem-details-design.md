# PRD: Respostas de erro padronizadas com Problem Details

**Status:** Implementado e mergeado em main — PR #4 (codex/problem-details-error-handling)
**Data:** 2026-06-28
**Branch alvo:** `feat/product-create` -> `hexagonal-architecture`

---

## Problema

O adaptador HTTP de produtos hoje converte `ProductValidationException` para HTTP 400 com blocos `try/catch` manuais dentro de `ProductController`. O corpo da resposta de erro fica a cargo do comportamento padrao do Spring para `ResponseStatusException`, entao a API ainda nao tem um contrato de erro explicito e documentado.

Isso gera tres problemas:

1. Os metodos do controller repetem a mesma regra de mapeamento de erro de validacao.
2. Clientes da API nao conseguem depender de um formato de resposta definido pelo projeto.
3. Erros inesperados de servidor podem usar respostas padrao do framework sem que isso esteja documentado como contrato da API.

O projeto precisa de uma unica fronteira de tratamento de erros HTTP que preserve a arquitetura hexagonal: os use cases continuam lancando excecoes da camada de aplicacao, enquanto o adaptador HTTP traduz essas excecoes para respostas estaveis.

---

## Objetivo

Implementar um `ControllerAdvice` global do Spring para retornar erros da API de forma consistente:

1. `ProductValidationException` retorna status 4xx, inicialmente `400 Bad Request`.
2. Respostas de validacao usam uma unica mensagem fail-fast, mantendo o comportamento atual dos use cases.
3. Corpos de erro usam RFC 7807 Problem Details.
4. Excecoes nao tratadas retornam `500 Internal Server Error` no mesmo formato.
5. Respostas 5xx nao vazam stack trace, nomes de classes de excecao, detalhes SQL ou detalhes internos de implementacao.
6. `ProductController` permanece fino e nao contem mapeamento manual de erros.

---

## Fora de escopo

- Nao mudar a validacao dos use cases de fail-fast para colecao de multiplos erros.
- Nao introduzir Bean Validation annotations como mecanismo principal de validacao.
- Nao alterar paginacao nem adicionar contagens totais.
- Nao criar uma taxonomia customizada de erros com URLs especificas no campo `type`.
- Nao expor detalhes de debug em respostas de erro.

---

## Decisao

Usar **RFC 7807 Problem Details** como DTO de resposta para erros da API.

Exemplo de resposta de validacao:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "sku must not be blank.",
  "instance": "/products"
}
```

`type` e o identificador do tipo de problema no padrao RFC 7807. Nesta primeira versao, ele deve permanecer como `about:blank`, que significa que o erro usa o significado padrao do status HTTP em vez de uma categoria especifica do projeto.

Na pratica:

- `status: 400` e `title: "Bad Request"` definem a categoria do erro de validacao.
- `detail` carrega a mensagem especifica retornada pelo use case.
- `instance` identifica o caminho da requisicao, como `/products`.
- `type: "about:blank"` evita criar uma taxonomia de erros antes de existir uma necessidade real.

Se o projeto precisar de categorias mais ricas no futuro, o campo `type` podera evoluir para URLs estaveis do projeto, por exemplo:

```json
{
  "type": "https://nexus-shopping.dev/problems/product-validation"
}
```

Isso fica propositalmente fora do escopo deste PRD.

---

## Arquitetura

O novo componente de tratamento de erros vive no adaptador HTTP inbound, porque sua responsabilidade e traduzir excecoes da aplicacao para respostas HTTP.

Pacote proposto:

```text
product/
  adapter/
    inbound/
      http/
        ProductController.kt
        ProductExceptionHandler.kt
```

`ProductExceptionHandler` deve usar `@RestControllerAdvice` ou `@ControllerAdvice` com `@ResponseBody`. Ele depende de tipos do Spring Web e pode referenciar `ProductValidationException`.

Isso preserva a direcao de dependencia:

```text
HTTP adapter -> application exception
application -> domain / outbound ports
domain -> sem dependencias de framework
```

Nenhuma classe de dominio ou use case deve depender de `ProblemDetail`, `HttpStatus` ou tipos de requisicao HTTP.

---

## Comportamento de erros

### Erros de validacao

`ProductValidationException` mapeia para:

- Status HTTP: `400 Bad Request`
- `type`: `about:blank`
- `title`: `Bad Request`
- `status`: `400`
- `detail`: mensagem da excecao
- `instance`: caminho da requisicao

O comportamento fail-fast atual permanece igual. Cada resposta contem uma unica string em `detail`, nao um array `errors`.

### Excecoes nao tratadas

Qualquer excecao nao tratada por um metodo mais especifico do advice mapeia para:

- Status HTTP: `500 Internal Server Error`
- `type`: `about:blank`
- `title`: `Internal Server Error`
- `status`: `500`
- `detail`: uma mensagem generica, como `Unexpected server error.`
- `instance`: caminho da requisicao

A resposta 500 nao deve expor:

- stack traces
- nomes de classes de excecao
- mensagens SQL
- detalhes de datasource
- caminhos de arquivos
- detalhes internos do framework

Os logs do servidor podem continuar registrando a excecao original para diagnostico.

### Erros 4xx do framework

A implementacao deve evitar converter acidentalmente respostas 4xx geradas pelo Spring em 500. Se o Spring lancar uma excecao que ja carrega um status HTTP, o advice deve preservar esse status quando for pratico.

Exemplos:

- JSON malformado -> 400
- metodo HTTP nao suportado -> 405
- media type nao suportado -> 415

Essas respostas tambem devem seguir Problem Details quando a implementacao conseguir fazer isso sem logica customizada ampla.

---

## Contrato da API

Falha de validacao:

```http
POST /products
Content-Type: application/json

{
  "brandId": 1,
  "categoryId": 1,
  "sku": "",
  "name": "Product Name",
  "slug": "product-name",
  "priceAmount": 49.90
}
```

Resposta:

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json
```

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "sku must not be blank.",
  "instance": "/products"
}
```

Erro inesperado de servidor:

```http
HTTP/1.1 500 Internal Server Error
Content-Type: application/problem+json
```

```json
{
  "type": "about:blank",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "Unexpected server error.",
  "instance": "/products"
}
```

---

## Estrategia de testes

Adicionar ou atualizar testes no nivel do controller para validar status code e corpo da resposta.

Casos recomendados:

1. `POST /products` com payload invalido retorna 400 Problem Details.
2. A resposta contem `type`, `title`, `status`, `detail` e `instance`.
3. `detail` contem a mensagem de validacao de `ProductValidationException`.
4. O controller nao precisa mais de blocos locais de `try/catch` para validacao.
5. Uma excecao inesperada vinda de um use case mockado ou caminho especifico de teste retorna 500 Problem Details.
6. O corpo da resposta 500 nao contem stack trace, nomes de classes de excecao nem mensagens internas.

O comando principal de verificacao continua sendo:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

---

## Criterios de aceite

- Todas as respostas de erro cobertas por este PRD usam RFC 7807 Problem Details.
- `ProductValidationException` retorna `400 Bad Request`.
- Respostas de validacao expoem exatamente uma mensagem fail-fast em `detail`.
- Excecoes nao tratadas retornam `500 Internal Server Error` com `detail` generico.
- `ProductController` nao captura manualmente `ProductValidationException`.
- Testes validam o contrato do corpo da resposta, nao apenas os status codes.
- O build da aplicacao passa com o comando Gradle Wrapper documentado acima.
