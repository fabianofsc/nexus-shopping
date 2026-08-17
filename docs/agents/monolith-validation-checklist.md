# Checklist de validacao e aceite (monolith-first)

Checklist end-to-end de toda a estrutura atual do monolito (branch `monolith-first`).
Organizado por dominio para permitir teste/validacao em paralelo. Cada dominio tem um
conjunto de verificacoes com **criterio de aceite**.

Duas camadas de validacao:

1. **Automatizada (autoritativa):** `./gradlew build` roda todos os testes (unit,
   integracao, HTTP, concorrencia, migrations, ArchUnit). Um dominio esta aceito se os
   testes do seu pacote passam.
2. **E2E ao vivo:** com `postgres`+`redis` + `bootRun` de pe, cada dominio e validado via
   HTTP (curl) com dados isolados, confirmando o comportamento real ponta a ponta.

Execucao ao vivo:

```bash
docker compose up -d postgres redis
./gradlew bootRun           # aplicacao em http://localhost:8080
./scripts/e2e-monolith-demo.sh
```

---

## Global / Plataforma

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| G1 | Health do Actuator | `GET /actuator/health` -> `200` com `"status":"UP"` |
| G2 | Flyway / migrations | App sobe sem erro; migrations portaveis H2+Postgres (testes passam) |
| G3 | Arquitetura (ArchUnit) | Regras hexagonais + isolamento de contextos passam |
| G4 | Contrato OpenAPI | `docs/api/openapi.yaml` parseia e cobre os endpoints atuais |
| G5 | Erros RFC 7807 | Erros retornam `application/problem+json` com type/title/status/detail/instance |
| G6 | Correlation-id / logs | Filtro de correlation-id ativo (ECS logging) |

---

## Product / Catalogo

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| P1 | Busca por categoria | `GET /products?categoryId=1&page=0&size=50` -> `200` slice `{content,page,size,count,hasNext}` |
| P2 | Busca por nome | `GET /products?name=Product%201&page=0&size=50` -> `200`, resultados com prefixo |
| P3 | Paginacao | `size` em `1..500`, `page>=0`; `size` invalido -> `400` |
| P4 | categoryId E name juntos | -> `400` ("Use either categoryId or name, not both") |
| P5 | Detalhe | `GET /products/{id}` -> `200`; inexistente -> `404` |
| P6 | Produto ARCHIVED | some da busca e do detalhe (detalhe -> `404`) |
| P7 | Criar produto | `POST /products` -> `201`; campo invalido -> `400` |
| P8 | Preco | `PATCH /products/{id}` com `priceAmount` -> `200`; `<=0` -> `400` |
| P9 | Metadados | `PATCH /products/{id}/details` -> `200` atualiza name/slug/description/brandId/categoryId |
| P10 | Archive | `POST /products/{id}/archive` -> `200`, status `ARCHIVED`; inexistente -> `404` |
| P11 | FK valida | Criar produto com `brandId`/`categoryId` inexistente ou categoria INACTIVE -> `400` |
| P12 | Cache Redis | `GET /products/{id}` repetido usa cache (testes de cache passam) |

## Brands / Categories

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| B1 | Listar marcas | `GET /brands` -> `200` array com as marcas do seed |
| B2 | Criar marca | `POST /brands` -> `201`; nome em branco -> `400` |
| B3 | Listar categorias | `GET /categories` -> `200` array |
| B4 | Criar categoria | `POST /categories` -> `201`; status invalido -> `400` |
| B5 | Status de categoria | `PATCH /categories/{id}/status` -> `200`; invalido -> `400`; inexistente -> `404` |
| B6 | Categoria INACTIVE esconde produtos | Busca por categoria INACTIVE nao retorna seus produtos |

## Customer

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| C1 | Criar cliente | `POST /customers` -> `201`; campo invalido -> `400` |
| C2 | Detalhe | `GET /customers/{id}` -> `200`; inexistente -> `404` |
| C3 | Endereco GET | `GET /customers/{customerId}/address` -> `200`; cliente inexistente -> `404` |
| C4 | Endereco PUT | `PUT /customers/{customerId}/address` sobrescreve -> `200`; campo invalido -> `400`; inexistente -> `404` |

## Cart

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| CA1 | Carrinho ativo | `GET /customers/{id}/cart` -> `200`, cria vazio se nao existe |
| CA2 | Add item | `POST /cart/items` -> `200`; soma quantidade se ja existe |
| CA3 | Update quantidade | `PUT /cart/items/{productId}` com `quantity` absoluto -> `200`; `0` remove; ausente -> `400`; negativo -> `400` |
| CA4 | Remove item | `DELETE /cart/items/{productId}` -> `200` |
| CA5 | Limpar | `DELETE /cart/items` -> `200` carrinho vazio |
| CA6 | Concorrencia | Um cliente tem no maximo um carrinho ACTIVE sob concorrencia (testes passam) |

## Checkout

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| CO1 | Checkout aprovado | `POST /customers/{id}/cart/checkout {paymentToken:"approved"}` + `Idempotency-Key` -> `201`, pedido `CONFIRMED`, estoque baixado, notificacao `SENT` |
| CO2 | Checkout rejeitado | token `rejected` -> `201`, pedido `PAYMENT_FAILED`, estoque liberado, sem notificacao |
| CO3 | Replay | Mesma `Idempotency-Key` e payload -> `200` (mesmo pedido, sem novo dispatch/baixa/notificacao) |
| CO4 | Conflito | Mesma chave com token diferente -> `409` |
| CO5 | Estoque insuficiente | -> `409`, sem pedido/carrinho alterado |
| CO6 | Carrinho vazio / ja fechado | -> `400` |
| CO7 | Sem `Idempotency-Key` | -> `400` |
| CO8 | Corpo apenas `paymentToken` | Resolve cliente/endereco do cadastro (sem snapshots inline) |

## Order

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| O1 | Listar pedidos | `GET /customers/{id}/orders` -> `200` slice paginado |
| O2 | Detalhe | `GET /customers/{id}/orders/{orderId}` -> `200`; de outro cliente -> `404` |
| O3 | Cancelar | `POST /customers/{id}/orders/{orderId}/cancel` (WAITING_PAYMENT) -> `200`, libera estoque |
| O4 | Cancelar indevido | Pedido CONFIRMED/PAYMENT_FAILED nao cancelavel -> `409` |

## Payment

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| PA1 | Aprovado | Via checkout `approved`, `payment_attempts.status = APPROVED` |
| PA2 | Rejeitado | Via checkout `rejected`, `payment_attempts.status = REJECTED` |
| PA3 | Idempotencia | Replay nao cria novo dispatch/payment_attempt (contagem = 1) |
| PA4 | Token opaco | Nenhum log/coluna expoe o `paymentToken` |

## Notification

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| N1 | Enviar | `POST /notifications` -> `201`, idempotente por `notificationKey` |
| N2 | Listar | `GET /notifications?customerId=` -> `200` slice paginado |
| N3 | Detalhe | `GET /notifications/{id}` -> `200`; inexistente -> `404` |
| N4 | Confirmacao de checkout | Checkout aprovado gera notificacao `SENT`; rejeitado nao gera |

## Inventory

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| I1 | Baixa no checkout | Checkout aprovado decrementa `products.inventory_quantity` |
| I2 | Liberacao em rejected | Checkout rejeitado devolve o estoque |
| I3 | Liberacao em cancelamento | Cancelar devolve o estoque |
| I4 | Insuficiente | Checkout com estoque insuficiente -> `409`, estoque nunca negativo (concorrencia) |
| I5 | Ledger | `stock_movements` registra DECREASE/RELEASE por referencia |

---

## Fluxo E2E completo (scripts/e2e-monolith-demo.sh)

O script cobre o caminho feliz e o de falha de ponta a ponta (cliente -> produto ->
carrinho -> checkout aprovado -> estoque/notificacao; checkout rejeitado -> estoque
liberado). Criterio: **todas as linhas terminam com "ok" e exit 0**.
