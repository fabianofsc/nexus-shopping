# Spec: Fechamento do monolito de e-commerce (baseline monolith-first)

**Status:** Proposta
**Data:** 2026-08-17
**Branch alvo:** `monolith-first`
**Pre-requisito:** estado atual `28dd006` (build e testes verdes)

## Objetivo

Transformar `monolith-first` no baseline monolito completo de e-commerce, fechando
os gaps funcionais (Inventory, enderecos, carrinho, catalogo) e tornando o baseline
reproduzivel em outras linguagens (contrato OpenAPI, script E2E e manual do aluno).
Payment e Notification permanecem em processo e sincronos; **retry de pagamento e
reconciliation agendada ficam explicitamente adiados** para a extracao do Payment
Service, pois dependem de um provider externo que nao existe neste baseline.

Mensagem didatica preservada:

```text
Decomposicao de dominio primeiro.
Distribuicao fisica depois.
```

## Estado atual (recap)

Monolito modular Kotlin, hexagonal (`adapter -> application -> domain`), 6 Bounded
Contexts em um banco compartilhado (11 migrations Flyway portaveis PostgreSQL/H2):

| Contexto | Estado |
| --- | --- |
| Product | Busca paginada (Slice sem COUNT) por categoria/nome, detalhe, create, update de preco, indexes, cache Redis |
| Customer | Create + get; Address/Contact persistidos, sem CRUD |
| Cart | Carrinho ACTIVE por cliente (lock `SELECT FOR UPDATE`), add/remove/clear/get, sem update de quantidade |
| Order | Snapshots historicos, estados `WAITING_PAYMENT/PAYMENT_PROCESSING/PAYMENT_FAILED/CONFIRMED/CANCELLED`, idempotencia por `idempotency_key + fingerprint` |
| Payment | `PaymentAttempt` idempotente, journal de dispatch, lease de processamento, provider simulado (logging), replay via mesma `Idempotency-Key` |
| Notification | Registro idempotente por `notification_key`, envio por email simulado, acionado no checkout |

Checkout orquestrado em `integration/checkout` por ports/ACLs (`CheckoutWorkflowUseCase`)
com replay idempotente e `Idempotency-Key` no HTTP. Infra com correlation-id (ECS),
Problem Details (RFC 7807) e NGINX LB com 3 instancias.

Gaps confirmados que este spec fecha:

- sem controle de estoque (overselling);
- sem CRUD de enderecos nem selecao de endereco no checkout;
- sem update de quantidade no carrinho;
- sem CRUD de marcas/categorias e sem archive de produto (busca nem filtra `ACTIVE`);
- sem contrato canônico de API, script E2E nem manual do aluno;
- `REQUESTED/202/lease/replay` existem mas nao estao documentados como gancho de extracao.

## Escopo

### Entra

1. Inventory: baixa/reserva atomica no checkout + liberacao em falha/cancelamento + ledger de movimentacoes.
2. Customer: CRUD de enderecos e selecao de endereco no checkout.
3. Cart: update de quantidade de item.
4. Catalogo: CRUD minimo de brands/categories, archive/update de produto e filtro `ACTIVE` na busca.
5. ADR de adiamento de retry/reconciliation.
6. `openapi.yaml` como contrato canonico.
7. Script E2E de demonstracao.
8. Manual do aluno (baseline reproduzivel).

### Nao entra (adiado para a evolucao distribuida)

- Retry de pagamento e reconciliation agendada (extracao do Payment Service).
- Carrinho anonimo, TTL/ABANDONED automatizado.
- Auth/Identity, API Gateway, broker/event-driven, saga, banco por servico.
- Devolucao/reembolso/chargeback, entrega/frete/fulfilment.
- Carrinho e enderecos novos exigem `customerId` confiavel (sem autenticacao, como hoje).

## Decisao de contrato: checkout passa a usar o endereco cadastrado do cliente

Hoje o `CheckoutRequest` exige `customerSnapshot` e `shippingAddressSnapshot` inline no
body. Isso faz o cliente digitar dados que ja deveriam estar cadastrados e impede o
workflow de validar o cadastro.

Decisao (revisada em 2026-08-17): o baseline usa **um unico endereco por cliente, com
sobrescrita** (`PUT /customers/{customerId}/address`). Nao ha lista de enderecos nesta
fase; multiplos enderecos (casa/trabalho) sao evolucao futura e nao trazem ganho
didatico para o baseline.

- `POST /customers/{customerId}/cart/checkout` passa a receber `{ paymentToken }`,
  sem snapshots inline.
- O workflow (camada `integration/checkout`) resolve o snapshot do cliente e do endereco
  consultando Customer por uma porta propria (`CustomerSnapshotPort`), validando que o
  cliente existe e possui endereco cadastrado, e passa os snapshots resolvidos para Order.
- `Order` continua persistindo os snapshots como fato historico imutavel e continua nao
  consultando Customer durante a criacao do pedido (vale para o futuro distribuido).
- Os testes HTTP existentes que enviam snapshots inline sao atualizados.

Motivo: prepara Auth (o `customerId` vira da identidade, nao do body), reduz superficie
de erro e mantem a fronteira Order/Customer do ADR.

## Feature 1: Inventory

### Modelo

Novo Bounded Context `inventory`:

```text
StockMovement
  id
  productId
  orderId?            # referencia opaca para rastreio
  quantity            # positivo = entrada, negativo = saida
  movementType        # DECREASE | RELEASE
  reference           # ex.: orderReference (opaco)
  createdAt
```

O estoque disponivel continua na coluna `products.inventory_quantity` (fonte unica de
verdade, de leitura publica no catalogo). O dominio de Inventory **nao importa** pacotes
de `product`/`order`; o adapter JPA de Inventory opera essa coluna por `UPDATE` condicional.

### Regras

- **Baixa no checkout (dentro da Transacao A do workflow):** para cada item, decremento
  atomico condicional, sem lock de leitura:

  ```sql
  UPDATE products
  SET inventory_quantity = inventory_quantity - :qty,
      updated_at = CURRENT_TIMESTAMP
  WHERE id = :productId
    AND inventory_quantity >= :qty
  ```

  Se algum item retornar 0 linhas afetadas -> rollback da Transacao A e erro
  `409 Conflict` (Problem Details) listando os produtos sem estoque. Nenhum pedido,
  nenhuma mudanca de carrinho e nenhuma movimentacao persistem.

- **Ledger:** cada baixa registra um `StockMovement` (`DECREASE`) na mesma transacao.
- **Liberacao em falha de pagamento:** quando o resultado do pagamento e `REJECTED` ou a
  ordem fica em estado final de falha, o estoque decrementado e devolvido
  (`RELEASE`, quantidade positiva) com a mesma referencia.
- **Liberacao em cancelamento:** `CancelOrderUseCase` restaura o estoque da ordem via
  porta outbound `ReleaseStockPort` (definida por Order, implementada pelo adapter de
  Inventory na composicao). O dominio de Order continua sem importar Inventory.
- **Simplificacao documentada:** no monolito, "reserva" e "baixa" colapsam no decremento
  atomico: o item fica comprometido para o pedido no momento do checkout e e liberado em
  falha/cancelamento. Reserva em duas fases e o padrao de saga ficam para a extracao.

### Concorrencia

O `UPDATE` condicional e atomico no banco: duas compras simultaneas do ultimo item
fazem apenas uma vencer. Testes de concorrencia com barreira (mesmo padrao de
`CartConcurrencyTest`) validam que nenhuma excecao de unicidade/estado escapa e que o
estoque nunca fica negativo.

## Feature 2: Customer — endereco unico com sobrescrita e uso no checkout

### Endpoints

```text
GET  /customers/{customerId}/address
PUT  /customers/{customerId}/address
```

- `GET` devolve o endereco cadastrado; cliente inexistente -> `404 Not Found`.
- `PUT` sobrescreve integralmente o endereco (todos os campos obrigatorios, mesmas
  validacoes do cadastro). Cliente inexistente -> `404 Not Found`; campo invalido -> `400`.
- CRUD segue o padrao hexagonal de Customer (use case valida, adapter nao valida).
- Nao ha DELETE nesta fase: o endereco e obrigatorio para checkout e o cadastro de
  cliente exige um endereco.

### Porta de resolucao para o checkout

Customer expoe (em `application/port/inbound`) uma operacao para o workflow resolver o
snapshot de cliente e endereco a partir do `customerId`, validando que o cliente existe
e possui endereco. O adapter local que implementa essa porta faz a leitura no banco
compartilhado; no futuro vira chamada HTTP ao Customer Service.

## Feature 3: Cart — update de quantidade

`PUT /customers/{customerId}/cart/items/{productId}` com body `{ "quantity": N }`.

Semantica:

- `N == 0` -> remove o item (equivalente a `DELETE`);
- produto fora do carrinho e `N > 0` -> adiciona com `ProductSummary` do catalogo;
- senao, ajusta a quantidade absoluta do item existente.

Reutiliza as regras de idempotencia e o lock de carrinho ACTIVE ja existentes. Ajusta o
preco total automaticamente (o carrinho ja calcula `totalAmount`).

## Feature 4: Catalogo — CRUD minimo

### Brands e Categories

```text
GET   /brands          GET   /categories
POST  /brands          POST  /categories
                      PATCH /categories/{id}/status   (ACTIVE/INACTIVE)
```

- Novos contextos? Nao: `brands`/`categories` continuam no contexto Product (sao parte
  do catalogo). Ganham controller + use cases no padrao existente.
- Busca por categoria deve considerar apenas categorias `ACTIVE`.

### Product

```text
POST  /products/{id}/archive   -> status ARCHIVED
PATCH /products/{id}           -> update parcial de name/slug/description/brandId/categoryId
```

- A busca (`findByCategoryId` e `findByNamePrefix`) passa a filtrar `status = 'ACTIVE'`.
- `getById` nao lista produto `ARCHIVED` para clientes (nao muda para admin; nao ha admin).

## Feature 5: ADR de adiamento de retry/reconciliation

Documento em `docs/decisions/2026-08-17-monolith-baseline-deferred-payment-retry.md`:

- Explica que `REQUESTED`/`202`/`WAITING_PAYMENT`/lease/`replay` ja existem e sao o
  contrato que antecipa a extracao do Payment.
- Registra a decisao: no monolito, o provider e em processo e sincrono
  (`LoggingPaymentProviderGateway` so retorna APPROVED/REJECTED), portanto o caminho de
  incerteza nao e alcancavel em producao; endpoint de retry e job de reconciliation
  so fazem sentido quando o Payment virar servico sobre DummyPay.

## Feature 6: Contrato canonico OpenAPI

Arquivo `docs/api/openapi.yaml` (OpenAPI 3.0), fonte de verdade do contrato HTTP:

- todos os endpoints existentes (catalogo, clientes, carrinho, checkout, pedidos, notificacoes);
- contrato de checkout com `Idempotency-Key`, status `200/201/202` e corpo de erro
  Problem Details (RFC 7807);
- estados de pedido e pagamento, regras de paginacao (Slice sem COUNT).

Objetivo: permitir que implementacoes Node/TypeScript, Java e Python repliquem o mesmo
comportamento a partir do contrato, com o Kotlin como implementacao de referencia.
Nenhuma geracao de codigo nesta fase; o arquivo e versionado e revisado como contrato.

## Feature 7: Script E2E

`scripts/e2e-monolith-demo.sh` (bash POSIX, `set -euo pipefail`, curl + asserts no estilo
de `scripts/test-lb.sh`), rodavel contra `docker compose up` local:

```text
criar cliente -> criar endereco -> adicionar itens ao carrinho -> checkout (token approved)
-> validar pedido CONFIRMED + notificacao + estoque baixado -> checkout (token rejected)
-> validar PAYMENT_FAILED + estoque liberado -> cancelar pedido -> validar estoque devolvido
```

## Feature 8: Manual do aluno

`docs/agents/monolith-baseline.md`, linkado no README:

- mapa de contextos e fronteiras (Product, Customer, Cart, Order, Payment, Notification, Inventory);
- estados e maquina de pedido/pagamento;
- chaves de idempotencia e regras de replay;
- como rodar e validar localmente;
- trilha de evolucao: o que o aluno implementa por stack (extrair Payment, event-driven
  com outbox, API Gateway, tracing distribuido, saga, banco por servico).

## Migrations

Nova migration `V12__create_inventory_ledger.sql` (portavel, H2 + PostgreSQL):

```sql
CREATE TABLE stock_movements (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL,
    order_id BIGINT,
    quantity INTEGER NOT NULL,
    movement_type VARCHAR(16) NOT NULL,
    reference VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_stock_movements_product_id ON stock_movements (product_id);
CREATE INDEX idx_stock_movements_reference ON stock_movements (reference);
```

Sem FK para `orders` (referencia opaca, portavel) nem mudanca em `products` (a coluna ja
existe). Address CRUD usa tabelas existentes de V4.

## Testes e conformidade

- Unit + integracao + HTTP por feature (padrao dos contextos atuais).
- Concorrencia de estoque com barreira; nunca estoque negativo.
- `PackageStructureArchitectureTest` ganha as regras de `inventory` (nao importa outros
  contextos; domain/application sem imports de framework).
- Contratos HTTP de checkout atualizados para resolver o endereco cadastrado do cliente.
- Migrations portaveis validas em H2 (testes) e PostgreSQL (runtime).
- Build final com `env GRADLE_USER_HOME=.../.gradle-local ./gradlew build`.

## Alternativas consideradas

### Inventory: coluna `products.inventory_quantity` vs tabela propria

- **Escolhida:** operar a coluna existente via `UPDATE` condicional no adapter. Uma
  fonte de verdade, portavel, minimo de schema. A extracao futura move a coluna para um
  servico de Inventory com ledger, mantendo o dominio pronto.
- Rejeitada: tabela `product_stock` + reserva em duas fases agora. Ensina o padrao de
  saga, mas adiciona complexidade de consistencia que so existe no distribuido.

### Checkout: snapshots inline vs `addressId`

- **Escolhida:** `addressId`. Dados cadastrados, validacao de posse, preparo para Auth.
- Rejeitada: manter inline. Continua permitindo dados ficticios e duplica o cadastro.

### Update de quantidade: PUT absoluto vs PATCH delta

- **Escolhida:** `PUT` com quantidade absoluta. Previsivel e idempotente, combina com as
  demais operacoes do carrinho.
- Rejeitada: PATCH com delta. Menos previsivel para o aluno e para o contrato.

## Consequencias

Positivas:

- Fluxo de compra completo e realista de ponta a ponta (catalogo -> carrinho -> checkout
  -> estoque -> pedido -> pagamento -> notificacao).
- Inventory introduz a aula classica de concorrencia (overselling) com lock/UPDATE atomico.
- Contrato canonico torna o baseline reproduzivel em Node/Java/Python.
- Adiamento de retry/reconciliation documentado evita construir incerteza que o monolito
  nao tem.

Trade-offs:

- Mudanca de contrato do checkout quebra os testes HTTP existentes (aceitavel no baseline).
- Inventory compartilha a coluna de produtos com Catalog (proxima fronteira de extracao).
- DELETE fisico de endereco e simplificado; regras de soft-delete ficam para evolucao.

Riscos:

- Escopo grande em uma unica PR. Mitigacao: tasks do plan agrupadas por feature, cada uma
  com commit proprio, revisao e merge humano no final.

## Checklist de aceite

- [ ] Checkout sem estoque retorna `409` com itens faltantes e nenhum efeito colateral.
- [ ] Checkout com `approved` baixa estoque, confirma pedido e notifica.
- [ ] Pagamento `rejected` libera o estoque; cancelamento devolve o estoque.
- [ ] Estoque nunca fica negativo sob concorrencia.
- [ ] Endereco unico com sobrescrita (`GET`/`PUT`); checkout usa o endereco cadastrado do cliente.
- [ ] Update de quantidade no carrinho (0 remove, novo adiciona, senal ajusta).
- [ ] Brands/categories CRUD minimo; busca filtra `ACTIVE`; archive de produto.
- [ ] `openapi.yaml` cobre todos os endpoints e contratos.
- [ ] `e2e-monolith-demo.sh` passa contra o compose local.
- [ ] Manual do aluno linkado no README.
- [ ] Build + testes + `ktlintCheck` verdes.
