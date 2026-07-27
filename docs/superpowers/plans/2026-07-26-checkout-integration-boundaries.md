# Checkout Integration Boundaries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remover o acoplamento Cart -> porta de Order e mover o checkout para um workflow de integracao com ACL, preservando HTTP, atomicidade local e idempotencia.

**Architecture:** `integration/checkout/application` e puro e depende apenas de gateways e `TransactionPort` proprios. Adapters locais traduzem DTOs de Integration para portas inbound de Cart e Order; Cart e Order nao se importam mutuamente.

**Tech Stack:** Kotlin, Spring Boot, Spring Data JPA, H2, PostgreSQL, Flyway, Gradle Wrapper e kotlin-test.

## Global Constraints

- Executar esta implementacao em branch/worktree isolada e abrir PR antes de Payment.
- Seguir `adapter -> application -> domain`; domain/application sem JPA, Hibernate ou Spring Data.
- Nenhum arquivo em `cart` importa `order`/`integration`; nenhum em `order` importa `cart`/`integration`.
- Apenas adapters de `integration` dependem de portas inbound dos contextos.

---

### Task 1: Portas inbound de Cart e Order

**Files:**

- Create: `cart/application/port/inbound/CartCheckoutInputPort.kt`
- Create: `order/application/port/inbound/CreateOrderInputPort.kt`
- Create: `cart/application/usecase/CartCheckoutUseCase.kt`
- Create: `order/application/usecase/CreateOrderUseCase.kt`
- Modify: `cart/adapter/outbound/jpa/CartJpaRepositoryAdapter.kt`
- Remove: `order/application/port/outbound/CartCheckoutPort.kt`

**Interfaces:**

```kotlin
interface CartCheckoutInputPort {
    fun reserveActiveCart(customerId: Long): CartCheckoutReservation
    fun confirmCheckout(reservationId: Long)
}

interface CreateOrderInputPort {
    fun create(command: CreateOrderCommand): CreatedOrder
}
```

- [ ] **Step 1: Escrever testes vermelhos.** Cobrir reserva ACTIVE, carrinho vazio/inexistente, criacao por snapshots e `replayed`.
- [ ] **Step 2: Executar os testes.**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*CartCheckout*' --tests '*CreateOrder*'
```

Esperado: falha por contratos/casos de uso inexistentes.

- [ ] **Step 3: Implementar a menor versao verde.** Cart retorna tipos de Cart; Order recebe somente snapshots/itens e conserva idempotencia.
- [ ] **Step 4: Rodar novamente e remover o acoplamento antigo.** Nenhum adapter de Cart implementa porta de Order.
- [ ] **Step 5: Commit.**

```bash
git add src/main src/test
git commit -m "refactor: expose cart and order checkout ports"
```

### Task 2: Workflow e ACL de Integration

**Files:**

- Create: `integration/checkout/application/CheckoutWorkflowUseCase.kt`
- Create: `integration/checkout/application/port/outbound/{CheckoutCartGateway,OrderCreationGateway,TransactionPort}.kt`
- Create: `integration/checkout/adapter/outbound/local/{LocalCheckoutCartGateway,LocalOrderCreationGateway,JpaTransactionAdapter}.kt`
- Create: `integration/checkout/adapter/inbound/http/{CheckoutController,dto/CheckoutRequest}.kt`
- Remove/move: `order/adapter/inbound/http/OrderCheckoutController.kt` e DTOs exclusivos.

**Interfaces:**

```kotlin
class CheckoutWorkflowUseCase(
    private val carts: CheckoutCartGateway,
    private val orders: OrderCreationGateway,
    private val transaction: TransactionPort,
) {
    fun execute(command: CheckoutCommand): CheckoutOrderData
}
```

- [ ] **Step 1: Escrever teste vermelho do workflow.** Provar a ordem `reserve -> create -> confirm`, rollback e propagacao de replay.
- [ ] **Step 2: Implementar gateways e workflow puros.** O workflow nunca importa Cart, Order ou adapters.
- [ ] **Step 3: Implementar ACL local e mover controller.** Preservar `POST /customers/{customerId}/cart/checkout`, `Idempotency-Key`, 201 e 200.
- [ ] **Step 4: Rodar os testes HTTP e do workflow.**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*Checkout*' --tests '*OrderControllerTest'
```

- [ ] **Step 5: Commit.**

```bash
git add src/main src/test
git commit -m "refactor: orchestrate checkout through integration workflow"
```

### Task 3: Transacao, arquitetura e regressao

**Files:**

- Modify: `PackageStructureArchitectureTest.kt`
- Create: `integration/checkout/CheckoutWorkflowIntegrationTest.kt`
- Modify: testes existentes de rollback e concorrencia.

- [ ] **Step 1: Escrever teste de arquitetura vermelho.** Proibir `cart.. -> order..|integration..`, `order.. -> cart..|integration..` e `integration.checkout.application.. -> cart..|order..|integration.checkout.adapter..`.
- [ ] **Step 2: Escrever teste de rollback real.** Forcar excecao apos reserva e conferir, no H2, que Cart continua ACTIVE e nao ha Order.
- [ ] **Step 3: Implementar `TransactionPort` em adapter de Integration.** Usar `@Transactional` no adapter e propagacao `REQUIRED`.
- [ ] **Step 4: Rodar a verificacao final.**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

- [ ] **Step 5: Commit, push e PR.**

```bash
git add src/main src/test
git commit -m "test: enforce checkout integration boundaries"
git push origin <branch>
```

**Stop condition:** Abrir PR e aguardar revisao/merge humano. Nao iniciar Payment antes de atualizar a worktree com a `main` que contem esse merge.

