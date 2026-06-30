# Reorganizacao de pacotes para crescimento hexagonal

**Status:** Pendente de implementacao
**Data:** 2026-06-30
**Branch alvo:** worktree isolada -> `hexagonal-architecture`

---

## Contexto

O modulo `product` ja segue arquitetura hexagonal, mas a estrutura atual ainda reflete
um unico dominio. Com a evolucao para ecommerce, o projeto tende a receber novos dominios
como carrinho, pedidos, clientes e pagamento.

Alguns sinais de crescimento ja aparecem:

- `application/usecase` contem use cases, commands e excecoes.
- `ProductExceptionHandler` esta no adaptador HTTP de produto, mas trata erros que serao
  comuns a toda API.
- `CreateProductRequest` e `UpdatePriceRequest` estao diretamente no pacote HTTP, sem um
  subpacote de DTOs.
- Controllers retornam entidades de dominio diretamente, acoplando o contrato HTTP ao
  modelo interno.
- A palavra `shared` foi rejeitada por risco de virar um pacote sem dono claro.

---

## Decisao

Adotar uma estrutura **feature-first por dominio** e criar um pacote transversal tecnico
chamado `platform`.

`platform` nao e um dominio de negocio. Ele existe apenas para contratos e adaptadores
tecnicos reutilizaveis por varios dominios, como excecoes base da aplicacao e tratamento
HTTP global.

Estrutura desejada para dominios:

```text
product/
  domain/
  application/
    command/
    exception/
    port/
      outbound/
    usecase/
  adapter/
    inbound/
      http/
        dto/
    outbound/
      jdbc/
```

Estrutura transversal permitida:

```text
platform/
  application/
    exception/
  adapter/
    inbound/
      http/
```

---

## Regras de responsabilidade

### Domain

`domain` deve conter entidades, value objects, paginas/modelos de dominio e regras que
existiriam mesmo fora da aplicacao HTTP.

`ProductValidationException` nao deve ficar em `domain`, pois hoje representa validacoes
de caso de uso e entrada da aplicacao, como paginacao, parametros mutuamente exclusivos e
campos obrigatorios.

`ProductNotFoundException` tambem nao deve ficar em `domain` nesta etapa. Produto nao
encontrado e uma falha de consulta/orquestracao do caso de uso, nao uma regra interna da
entidade `Product`.

### Application

`application/usecase` deve conter apenas casos de uso.

`application/command` deve conter objetos de entrada de escrita, como:

- `CreateProductCommand`
- `UpdatePriceCommand`

Novos pacotes irmaos, como `query` ou `result`, so devem ser criados quando houver modelos
reais que justifiquem essa separacao.

`application/exception` deve conter excecoes especificas do dominio quando elas preservam
linguagem util do caso de uso, como:

- `ProductValidationException`
- `ProductNotFoundException`

Essas excecoes devem estender tipos base de `platform.application.exception`.

### Platform

`platform` deve permanecer pequeno e tecnico.

Permitido:

- Excecoes base de aplicacao sem dependencia de framework.
- Handler HTTP global que traduz excecoes para Problem Details.
- Pequenas funcoes/classes de suporte HTTP se forem usadas por multiplos dominios.

Nao permitido:

- Regras de negocio.
- DTOs especificos de dominio.
- Repositorios.
- Mapeadores de entidade de dominio.
- Utilitarios genericos sem dono claro.

---

## Erros e Problem Details

Criar excecoes base em `platform.application.exception`:

- `ApplicationException`
- `ValidationException`
- `NotFoundException`

As excecoes especificas de produto passam a herdar desses tipos:

- `ProductValidationException : ValidationException`
- `ProductNotFoundException : NotFoundException`

O handler HTTP deve ser global e viver em:

```text
platform/adapter/inbound/http/ApiExceptionHandler.kt
```

Ele deve mapear:

- `ValidationException` -> `400 Bad Request`
- `NotFoundException` -> `404 Not Found`
- erros 4xx do Spring -> status HTTP correspondente
- excecoes inesperadas -> `500 Internal Server Error` com detalhe generico

O formato RFC 7807 Problem Details permanece o contrato de erro da API.

---

## DTOs HTTP e mapeamento

Requests e responses HTTP devem viver em:

```text
product/adapter/inbound/http/dto/
```

As funcoes de conversao HTTP devem ser extension functions colocalizadas nos proprios
arquivos de request/response dentro de `dto/`. Exemplos:

- `CreateProductRequest.toCommand()`
- `UpdatePriceRequest.toCommand(id)`
- `Product.toResponse()`, no arquivo de `ProductResponse`
- `ProductPage.toResponse()`, no arquivo de `ProductPageResponse`

Decisao de implementacao: extensions colocalizadas em `dto/`; pacote `mapper/` diferido.

O controller deve converter:

```text
HTTP request DTO -> application command -> use case -> domain model -> HTTP response DTO
```

Isso evita que o contrato HTTP dependa diretamente de `Product` e `ProductPage`.

Nesta refatoracao, o JSON de sucesso deve permanecer compativel com o contrato atual.

---

## Fora de escopo

- Criar novos dominios de ecommerce.
- Alterar migrations ou modelo de banco.
- Alterar formato RFC 7807 das respostas de erro.
- Alterar regras de validacao.
- Introduzir JPA ou outro mecanismo de persistencia.
- Criar taxonomia rica de error codes.

---

## Criterios de aceite

- O projeto nao possui pacote `shared` para esta reorganizacao.
- `platform` contem apenas responsabilidades tecnicas transversais.
- `ProductExceptionHandler` deixa de existir como handler especialista de produto.
- O handler global nao importa excecoes `Product*`.
- Commands saem de `application/usecase` e vao para `application/command`.
- Excecoes especificas de produto ficam em `product/application/exception`.
- Requests e responses HTTP ficam em `product/adapter/inbound/http/dto`.
- Controllers retornam DTOs HTTP, nao entidades de dominio diretamente.
- O contrato JSON dos endpoints existentes permanece compativel.
