# Payment Context Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar Payment autonomo com provider simulado, idempotencia/fencing e checkout que confirma Order e assegura Notification.

**Architecture:** Payment possui tipos, ports e persistencia proprios; nenhum pacote Payment importa outros contextos. `integration/checkout` usa gateways/ACL para validar/processar Payment, aplicar resultado em Order e assegurar Notification. Provider e e-mail apenas registram envios.

**Tech Stack:** Kotlin, Spring Boot, Spring Data JPA, Flyway, H2, PostgreSQL, Gradle Wrapper e kotlin-test.

## Global Constraints

- Comecar somente apos merge humano da PR do plano `checkout-integration-boundaries` e `git pull` da `main`.
- Executar em branch/worktree isolada e abrir uma unica PR de Payment; aguardar revisão e merge humano.
- Payment nao importa `order`, `cart`, `customer`, `notification` ou `integration`, nem possui FK externa.
- Tokens nao sao persistidos/logados; Notification e best effort com deduplicacao, nunca exactly-once sem outbox.

---

### Task 1: Dominio, contratos e validacao

**Files:**

- Create: `payment/domain/{PaymentAttempt,PaymentStatus,PaymentAmount,PaymentCurrency,PaymentProvider}.kt`
- Create: `payment/application/{command,exception,port/inbound,port/outbound,usecase}/...`
- Test: `payment/domain/*Test.kt`, `payment/ProcessPaymentUseCaseTest.kt`

**Interfaces:**

```kotlin
interface ProcessPaymentInputPort {
    fun process(command: ProcessPaymentCommand): PaymentProcessingResult
}

interface PaymentProviderGateway {
    fun process(request: ProviderProcessingRequest): ProviderProcessingResult
}
```

- [ ] **Step 1: Escrever testes vermelhos.** Cobrir valor/moeda proprios, token opaco, HMAC, conflito e REQUESTED/APPROVED/REJECTED.
- [ ] **Step 2: Executar os testes.** Esperado: falha por classes inexistentes.
- [ ] **Step 3: Implementar versao verde.** Gerar `attemptReference`, `authorizationFingerprint` HMAC e `providerDispatchKey` SHA-256 versionada/tamanho-prefixado.
- [ ] **Step 4: Rodar testes e commit.**

```bash
git add src/main src/test
git commit -m "feat: add payment domain and processing ports"
```

### Task 2: Persistencia, reserva e fencing

**Files:**

- Create: `V9__create_payment_context.sql`
- Create: Payment entities, Spring Data repositories e `PaymentJpaRepositoryAdapter`
- Test: migration, adapter JPA e concorrencia de reserva.

- [ ] **Step 1: Escrever testes vermelhos.** Exigir unicidade `(reference_id, idempotency_key)`, `attempt_reference` unico, fingerprint persistido e claim condicional por `processingLeaseToken`.
- [ ] **Step 2: Implementar migration portavel e adapter.** `reserve` retorna `Created|Existing`; conflito relê vencedor em transacao limpa; `complete` atualiza somente com token atual.
- [ ] **Step 3: Escrever teste concorrente com barreira.** Nenhuma excecao de unicidade escapa e dono obsoleto nao finaliza tentativa reclamada.
- [ ] **Step 4: Commit.**

```bash
git add src/main/resources/db/migration src/main src/test
git commit -m "feat: persist idempotent payment attempts"
```

### Task 3: Provider simulado e journal tecnico

**Files:**

- Create: `payment/adapter/outbound/provider/LoggingPaymentProviderGateway.kt`
- Create: journal JPA e migration `payment_provider_dispatches`
- Test: `LoggingPaymentProviderGatewayTest.kt`

- [ ] **Step 1: Escrever teste vermelho.** `approved` gera APPROVED e log sem token; outro token gera REJECTED; mesma `providerDispatchKey` gera unico log; referencias distintas com mesma chave HTTP nao colidem.
- [ ] **Step 2: Implementar journal persistente.** Unicidade por `provider_dispatch_key`; primeiro registro decide resultado e os demais retornam resultado armazenado.
- [ ] **Step 3: Executar testes e commit.**

```bash
git add src/main/resources/db/migration src/main src/test
git commit -m "feat: add idempotent logging payment provider"
```

### Task 4: Aplicar resultado em Order e deduplicar Notification

**Files:**

- Modify: `order/domain/Order.kt`, JPA/migration e portas inbound de resultado.
- Modify: Notification domain, command, entity, repository, migration e `SendNotificationUseCase`.
- Test: transicoes idempotentes de Order e lifecycle `PENDING -> SENDING -> SENT|FAILED`.

- [ ] **Step 1: Escrever testes vermelhos.** Order aplica mesmo `attemptReference` como no-op; Notification reclama `notificationKey` por lease e dono obsoleto nao sobrescreve estado.
- [ ] **Step 2: Implementar persistencia sem FK cruzada.** Order armazena referencia opaca; Notification tem `notification_key` unico, `sending_lease_token` e update condicional.
- [ ] **Step 3: Cobrir falha antes/depois de envio.** Reclaim pode repetir log apos queda externa; o contrato permanece best effort.
- [ ] **Step 4: Commit.**

```bash
git add src/main/resources/db/migration src/main src/test
git commit -m "feat: apply payment results and deduplicate notifications"
```

### Task 5: Integrar checkout e verificar contratos

**Files:**

- Modify: gateways/workflow/adapters de `integration/checkout`.
- Modify: DTO HTTP de checkout para `paymentToken`.
- Modify: `PackageStructureArchitectureTest.kt`.
- Test: checkout HTTP, replay, 202 REQUESTED e concorrencia ponta a ponta.

- [ ] **Step 1: Escrever testes HTTP vermelhos.** `approved` retorna CONFIRMED/Notification SENT; rejeicao retorna PAYMENT_FAILED sem Notification; REQUESTED retorna 202 com `OrderResponse` WAITING_PAYMENT.
- [ ] **Step 2: Implementar gateways da ACL.** `PaymentValidationGateway`, `PaymentProcessingGateway`, `OrderPaymentResultGateway` e `NotificationGateway` usam somente DTOs de Integration.
- [ ] **Step 3: Implementar reconciliacao.** Replay nunca repete provider; sempre aplica resultado terminal e assegura Notification por chave de evento.
- [ ] **Step 4: Adicionar regras arquiteturais e concorrencia.** Application de Integration nao importa contextos; N requisicoes iguais produzem um dispatch externo durante lease e respostas consistentes.
- [ ] **Step 5: Verificacao final, commit, push e PR.**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
git add src/main src/test src/main/resources/db/migration
git commit -m "feat: integrate payment into checkout"
git push origin <branch>
```

**Stop condition:** Abrir PR, aguardar revisão e merge humano. Somente depois atualizar a `main` local e decidir a proxima evolucao.

