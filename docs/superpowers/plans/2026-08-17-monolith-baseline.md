# Monolith Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fechar o monolito de e-commerce do baseline `monolith-first`: Inventory (baixa/liberacao com ledger), CRUD de enderecos + `addressId` no checkout, update de quantidade no carrinho, CRUD minimo de catalogo (brands/categories/archive + filtro ACTIVE), ADR de adiamento de retry/reconciliation, contrato OpenAPI, script E2E e manual do aluno.

**Architecture:** Cada novo contexto segue hexagonal (`adapter -> application -> domain`). `inventory` nao importa outros contextos e opera `products.inventory_quantity` por `UPDATE` condicional; Order define `ReleaseStockPort` implementada pelo adapter de Inventory. O workflow de checkout resolve `addressId` via porta do Customer. Payment/Notification permanecem em processo e sincronos.

**Tech Stack:** Kotlin, Spring Boot 4.1, Spring Data JPA, Flyway, Redis, H2 (testes), PostgreSQL, Gradle Wrapper, kotlin-test, ArchUnit.

## Global Constraints

- Executar em worktree/branch isolada e abrir uma unica PR de fechamento do monolito; aguardar revisao e merge humano.
- `domain/` e `application/` sem imports de Spring/JPA/Hibernate; validacao nos use cases; `@Query` JPQL explicito; migrations portaveis (H2 + PostgreSQL).
- `inventory` nao importa `product`, `order`, `cart`, `customer`, `notification` ou `integration`.
- Tokens nunca persistidos/logados; `customerId` continua confiavel (sem auth).
- Contrato HTTP do checkout muda para `addressId`; testes existentes de snapshot inline sao atualizados.
- Nao implementar retry de pagamento nem reconciliation agendada (registrar no ADR).

---

### Task 1: Inventory - dominio, portas e contrato

**Files:**

- Create: `inventory/domain/{StockMovement,MovementType}.kt`, `inventory/application/port/outbound/StockLedgerPort.kt`, `inventory/application/port/inbound/DecrementStockInputPort.kt`
- Create: `order/application/port/outbound/ReleaseStockPort.kt` (porta do Order, nao de Inventory)
- Test: `inventory/domain/*Test.kt`

**Interfaces:**

```kotlin
interface StockLedgerPort {
    fun record(movement: StockMovement)
    fun releaseByReference(reference: String, productId: Long, quantity: Int)
}

interface ReleaseStockPort {
    fun release(orderReference: String, items: List<ReleasedItem>)
}
```

- [ ] **Step 1: Escrever testes vermelhos.** MovementType `DECREASE/RELEASE`; quantidade sinal/positivo; referencia opaca obrigatoria.
- [ ] **Step 2: Executar os testes.** Esperado: falha por classes inexistentes.
- [ ] **Step 3: Implementar dominio e portas verdes.**
- [ ] **Step 4: Rodar testes e commit.**

```bash
git add src/main src/test
git commit -m "feat: add inventory domain and stock ports"
```

### Task 2: Inventory - persistencia e decremento atomico

**Files:**

- Create: `V12__create_inventory_ledger.sql` (tabela `stock_movements` da spec)
- Create: `inventory/adapter/outbound/jpa/{StockMovementEntity,SpringDataStockMovementRepository,StockLedgerJpaRepositoryAdapter}.kt`
- Create: use case `DecrementStockUseCase` + adapter JPA que roda o `UPDATE` condicional sobre `products.inventory_quantity`
- Test: migration contract, adapter JPA, `DecrementStockUseCaseTest`

- [ ] **Step 1: Escrever testes vermelhos.** `UPDATE ... WHERE inventory_quantity >= :qty`; 0 linhas afetadas sinaliza falta de estoque; ledger grava `DECREASE` na mesma transacao; migration portavel.
- [ ] **Step 2: Implementar migration e adapter.** Decremento por item com atomicidade de banco; registro no ledger; nenhuma linha quando nao ha estoque.
- [ ] **Step 3: Escrever teste de concorrencia com barreira.** Duas compras do ultimo item -> uma vence; estoque nunca negativo; sem excecao de unicidade escapando.
- [ ] **Step 4: Commit.**

```bash
git add src/main src/test src/main/resources/db/migration
git commit -m "feat: persist stock movements with atomic decrement"
```

### Task 3: Inventory - integracao no checkout e liberacao

**Files:**

- Modify: `integration/checkout` (gateway/ACL `InventoryGateway`, workflow)
- Modify: `order` (use case `CancelOrderUseCase` chama `ReleaseStockPort`)
- Create: adapter de composicao que implementa `ReleaseStockPort` via `StockLedgerPort`/JPA
- Test: checkout HTTP (sem estoque -> 409, approved baixa, rejected libera), cancelamento devolve, replay nao decrementa duas vezes

- [ ] **Step 1: Escrever testes HTTP vermelhos.** 409 com itens faltantes e zero efeito; `approved` baixa e `CONFIRMED`; `rejected` libera; cancel devolve.
- [ ] **Step 2: Implementar gateway da ACL.** Decremento dentro da Transacao A (antes de criar Order); liberacao no resultado `REJECTED` e no cancelamento.
- [ ] **Step 3: Garantir idempotencia.** Replay de checkout nao decrementa estoque de novo.
- [ ] **Step 4: Commit.**

```bash
git add src/main src/test
git commit -m "feat: integrate inventory into checkout and cancellation"
```

### Task 4: Customer - endereco unico com sobrescrita

**Files:**

- Create: `customer/adapter/inbound/http/CustomerAddressController.kt` + `dto/UpdateCustomerAddressRequest.kt`
- Create: use case `UpdateCustomerAddressUseCase` + command; port `updateAddress` no `CustomerRepositoryPort` e adapter JPA
- Test: `CustomerAddressControllerTest.kt`, `UpdateCustomerAddressUseCaseTest.kt`

- [ ] **Step 1: Escrever testes vermelhos.** `GET /customers/{customerId}/address` devolve o endereco; `PUT` sobrescreve; cliente inexistente -> 404; campo invalido -> 400.
- [ ] **Step 2: Implementar use case e controller no padrao de Customer (endereco unico, sem lista).**
- [ ] **Step 3: Commit.**

```bash
git add src/main src/test
git commit -m "feat: add customer address get and update"
```

### Task 5: Customer - resolucao de snapshot e contrato sem snapshots inline no checkout

**Files:**

- Create: porta inbound/outbound de resolucao de snapshot do Customer (valida cliente existente com endereco)
- Modify: `integration/checkout` (`CheckoutRequest`, `CheckoutWorkflowUseCase`, gateways da ACL)
- Modify: testes HTTP de checkout (todos os cenarios removem snapshots inline)

- [ ] **Step 1: Escrever testes HTTP vermelhos.** Checkout de cliente sem endereco -> 404/400; valido -> cria Order com snapshots resolvidos.
- [ ] **Step 2: Implementar resolucao.** Workflow resolve customer/address via porta do Customer; passa snapshots para Order; Order segue imutavel.
- [ ] **Step 3: Atualizar todos os testes de checkout/replay/202/concorrencia para o novo contrato.**
- [ ] **Step 4: Commit.**

```bash
git add src/main src/test
git commit -m "feat: resolve checkout customer address from registration"
```

### Task 6: Cart - update de quantidade

**Files:**

- Modify: `cart` (use case `UpdateCartItemQuantityUseCase`, comando, `Cart.withItemQuantity`, controller `PUT /cart/items/{productId}`, DTO)
- Test: `CartControllerTest` + use case + `CartTest` (0 remove, ausente -> 400, ajusta absoluto)

- [ ] **Step 1: Escrever testes vermelhos.** Semantica 0/ausente/existente; mesmo lock e idempotencia do carrinho ACTIVE; sem consulta ao Catalogo.
- [ ] **Step 2: Implementar use case e endpoint reutilizando o lock de `updateCart`.**
- [ ] **Step 3: Commit.**

```bash
git add src/main src/test
git commit -m "feat: update cart item quantity"
```

### Task 7: Catalogo - Product archive, metadados e filtro ACTIVE

**Files:**

- Modify: `product` (use cases `ArchiveProductUseCase`/`UpdateProductDetailsUseCase`, commands, `ProductController` `POST /{id}/archive` e `PATCH /{id}/details`, `ProductRepositoryPort` + adapter JPA)
- Modify: `SpringDataProductRepository` busca filtra `status = ACTIVE` (`findByCategoryId`, `findByNamePrefix`)
- Modify: `ProductGetByIdUseCase` oculta `ARCHIVED`
- Test: HTTP + use cases + adapter + busca com ARCHIVED oculto

Nota: brands/categories ficam adiados (greenfield); escopo focado em Product.

- [ ] **Step 1: Escrever testes vermelhos.** Archive muda status; `PATCH /details` atualiza; busca filtra `ACTIVE`; detalhe oculta `ARCHIVED`; 404s.
- [ ] **Step 2: Implementar use cases, controller, port/adapter e filtro nas queries (manter Slice, sem COUNT).**
- [ ] **Step 3: Atualizar fakes e rodar testes.**
- [ ] **Step 4: Commit.**

```bash
git add src/main src/test
git commit -m "feat: catalog product archive and details update with active-only search"
```

### Task 8: ADR de adiamento de retry/reconciliation

**Files:**

- Create: `docs/decisions/2026-08-17-monolith-baseline-deferred-payment-retry.md`

- [ ] **Step 1: Escrever o ADR.** Justifica adiamento com o provider sincrono em processo; documenta `REQUESTED/202/lease/replay` como gancho da extracao; lista quando retry/reconciliation entram.
- [ ] **Step 2: Commit.**

```bash
git add docs/decisions
git commit -m "docs: defer payment retry and reconciliation to extraction"
```

### Task 9: Contrato canonico OpenAPI

**Files:**

- Create: `docs/api/openapi.yaml`

- [ ] **Step 1: Escrever o OpenAPI 3.0** cobrindo catalogo, clientes, carrinho, checkout (`Idempotency-Key`, 200/201/202, Problem Details), pedidos, notificacoes, inventory no checkout e CRUD de enderecos.
- [ ] **Step 2: Revisar nomes de schema** alinhados ao manual do aluno (Feature 8) e aos DTOs existentes.
- [ ] **Step 3: Commit.**

```bash
git add docs/api
git commit -m "docs: add canonical openapi contract"
```

### Task 10: Script E2E

**Files:**

- Create: `scripts/e2e-monolith-demo.sh`

- [ ] **Step 1: Escrever o script** (bash, `set -euo pipefail`, curl, asserts no estilo de `scripts/test-lb.sh`): cliente -> endereco -> carrinho -> checkout approved -> verificar CONFIRMED/estoque/notificacao -> checkout rejected -> verificar PAYMENT_FAILED/estoque liberado -> cancelar -> verificar estoque devolvido.
- [ ] **Step 2: Testar contra `docker compose up` local e ajustar asserts.**
- [ ] **Step 3: Commit.**

```bash
chmod +x scripts/e2e-monolith-demo.sh
git add scripts
git commit -m "test: add end-to-end monolith demo script"
```

### Task 11: Manual do aluno e README

**Files:**

- Create: `docs/agents/monolith-baseline.md`
- Modify: `README.md` (link para o manual, mapa com Inventory)

- [ ] **Step 1: Escrever o manual** (mapa de contextos, estados, idempotencia, como rodar, trilha de evolucao por stack).
- [ ] **Step 2: Atualizar README** e conferir que continua < 200 linhas de regras no CLAUDE.md (nada a mudar la).
- [ ] **Step 3: Commit.**

```bash
git add docs README.md
git commit -m "docs: add monolith baseline student guide"
```

### Task 12: Verificacao final e PR

- [ ] **Step 1: Rodar build completo com testes e ktlint.**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build ktlintCheck
```

- [ ] **Step 2: Revisar `git log` por contexto (code/migrations/tests/docs/scripts) e ajustar commits se necessario.**
- [ ] **Step 3: Atualizar a checklist de aceite da spec.**
- [ ] **Step 4: Push e abrir a PR; aguardar revisao e merge humano.**

```bash
git push -u origin docs/monolith-baseline-spec-plan
gh pr create --base monolith-first --title "Close monolith baseline (Inventory, addresses, cart, catalog, contract)"
```
