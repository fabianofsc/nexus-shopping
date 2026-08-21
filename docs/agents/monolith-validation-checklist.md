# Checklist de validacao e aceite

Checklist end-to-end de toda a estrutura atual do Nexus Shopping.
Organizado por dominio para permitir teste/validacao em paralelo. Cada dominio tem um
conjunto de verificacoes com **criterio de aceite**.

> **Pagamento assincrono.** O Payment foi extraido para o `nexus-payment-service`. O
> checkout responde `202` com o pedido em `WAITING_PAYMENT`; o resultado terminal
> (`CONFIRMED` / `PAYMENT_FAILED`), a notificacao e a devolucao de estoque acontecem
> depois, pela reconciliacao (`PaymentReconciliationScheduler`, ~2s de intervalo, com o
> DummyPay levando ~3s para processar). Todo criterio de aceite de checkout abaixo
> pressupoe **aguardar o pedido sair de `WAITING_PAYMENT`** antes de conferir efeitos.

Duas camadas de validacao:

1. **Automatizada (autoritativa):** `./gradlew build` roda todos os testes (unit,
   integracao, HTTP, concorrencia, migrations, ArchUnit). Um dominio esta aceito se os
   testes do seu pacote passam.
2. **E2E ao vivo:** com `postgres`+`redis` + `bootRun` de pe, cada dominio e validado via
   HTTP (curl) com dados isolados, confirmando o comportamento real ponta a ponta.

Execucao ao vivo:

```bash
docker compose down -v      # bases antigas quebram: V9/V10/V11 mudaram
docker compose up -d        # postgres, redis, nexus-payment-service, dummypay
./gradlew bootRun           # aplicacao em http://localhost:8080
./scripts/e2e-monolith-demo.sh
```

---

## Global / Plataforma

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| G1 | Health do Actuator | `GET /actuator/health` -> `200` com `"status":"UP"` |
| G2 | Flyway / migrations | App sobe sem erro; migrations portaveis H2+Postgres (testes passam). Base preexistente do baseline **precisa** de `docker compose down -v`: V9 mudou de checksum, V10 mudou de nome e a V11 antiga saiu |
| G3 | Arquitetura (ArchUnit) | Regras hexagonais + isolamento de contextos passam |
| G4 | Contrato OpenAPI | `docs/api/openapi.yaml` parseia e cobre os endpoints atuais |
| G5 | Erros RFC 7807 | Erros retornam `application/problem+json` com type/title/status/detail/instance |
| G6 | Correlation-id / logs | Filtro de correlation-id ativo (ECS logging) |
| G7 | Dependencia externa | `nexus-payment-service` e `dummypay` no ar (`docker compose ps`); sem eles o checkout falha apos reservar estoque |

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
| CO1 | Checkout aprovado | `POST /customers/{id}/cart/checkout {paymentToken:"card_processing_approved"}` + `Idempotency-Key` -> `202`, pedido `WAITING_PAYMENT`, estoque ja baixado. Apos a reconciliacao: `CONFIRMED` e notificacao `SENT` |
| CO2 | Checkout recusado | token `card_declined` -> `202`, pedido `WAITING_PAYMENT`. Apos a reconciliacao: `PAYMENT_FAILED`, estoque liberado, sem notificacao |
| CO3 | Replay | Mesma `Idempotency-Key` e payload -> mesmo pedido, sem novo dispatch/baixa/notificacao. `202` enquanto o pedido estiver em `WAITING_PAYMENT`; `200` depois que a reconciliacao o levou a um status terminal |
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
| PA1 | Aprovado | Via checkout `card_processing_approved`, `payment_attempts.status` e `REQUESTED` de imediato e **eventualmente** `APPROVED`, apos a reconciliacao |
| PA2 | Recusado | Via checkout `card_declined`, `payment_attempts.status` eventualmente `REJECTED` |
| PA3 | Idempotencia | Replay nao cria novo `payment_attempt` (contagem = 1) e nao gera segundo `POST /v1/payments` no provider (verificar por WireMock nos testes; a tabela `payment_provider_dispatches` nao existe mais — a dedup vive no header `Idempotency-Key` enviado ao servico) |
| PA4 | Token opaco | Nenhum log/coluna expoe o `paymentToken` |

## Notification Service e journal

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| N1 | Listar journal | `GET /backoffice/notification-submissions` -> `200` slice sem destinatario ou corpo |
| N2 | Retry | `POST /backoffice/notification-submissions/{id}/retry` reutiliza payload e `Idempotency-Key` persistidos |
| N3 | Descartar | `POST /backoffice/notification-submissions/{id}/discard` exige justificativa e encerra a submissao |
| N4 | Confirmacao de checkout | Aprovacao reserva uma submissao; depois de Billing e Shipping, o dispatch remoto aceita ou registra `FAILED` sem desfazer o pedido |

## Inventory

| # | Verificacao | Criterio de aceite |
| --- | --- | --- |
| I1 | Baixa no checkout | O checkout decrementa `products.inventory_quantity` de forma **sincrona**, dentro da transacao que cria o pedido (antes do dispatch do pagamento) |
| I2 | Liberacao em recusa | A devolucao do estoque e **assincrona**: acontece em `PaymentReconciliationUseCase` quando o outcome recusado transiciona o pedido. Repetir o ciclo de polling nao pode liberar duas vezes |
| I3 | Liberacao em cancelamento | Cancelar devolve o estoque |
| I4 | Insuficiente | Checkout com estoque insuficiente -> `409`, estoque nunca negativo (concorrencia) |
| I5 | Ledger | `stock_movements` registra DECREASE/RELEASE por referencia |

---

## Fluxo E2E completo (scripts/e2e-monolith-demo.sh)

O script cobre o caminho feliz e o de falha de ponta a ponta (cliente -> produto ->
carrinho -> checkout despachado `202` -> espera a reconciliacao -> estoque/notificacao;
checkout recusado -> espera a reconciliacao -> estoque liberado). Ele faz polling em
`GET /customers/{id}/orders/{orderId}` com `POLL_TIMEOUT` (default 30s) e usa os tokens
de cenario do DummyPay (`TOKEN_APPROVED`, `TOKEN_DECLINED`).
Criterio: **todas as linhas terminam com "ok" e exit 0**.
