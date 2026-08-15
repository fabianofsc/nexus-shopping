# Nexus Payment Service Adapter (Polling) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar um adapter outbound real (`NexusPaymentServiceProviderGateway`) que fale
HTTP com o microservice `nexus-payment-service`, coexistindo com o `LoggingPaymentProviderGateway`
atras de um toggle de configuracao, e fechar o ciclo assincrono via um scheduler de
reconciliacao (polling) em vez de um webhook receiver.

**Architecture:** `payment` ganha um segundo metodo no `PaymentProviderGateway`
(`checkStatus`), um campo novo em `PaymentAttempt` (`providerAttemptReference`) e um novo
caso de uso de reconciliacao (`ReconcilePendingPaymentAttemptsUseCase`). `integration/checkout`
ganha um scheduler (`adapter/inbound/scheduler`) que dirige essa reconciliacao e aplica o
resultado ao `Order` via ACL, exatamente como o workflow sincrono ja faz hoje. Nenhum pacote
`payment` ou `order` importa o outro; toda travessia continua via ports + ACL de
`integration/checkout`.

**Tech Stack:** Kotlin, Spring Boot 4, Spring Data JPA, Flyway, H2 (testes), PostgreSQL,
`org.springframework.web.client.RestClient`, Gradle Wrapper, kotlin-test.

**Referencia de spec:** `docs/superpowers/specs/2026-08-13-nexus-payment-service-adapter-design.md`
— ler antes de comecar, inclusive a nota sobre a dependencia externa (contrato de polling do
nexus-payment-service ainda em definicao em sessao paralela).

## Global Constraints

- Nao comecar a implementacao sem confirmar que a sessao paralela do nexus-payment-service ja
  fechou (ou nao muda) o contrato de `GET /v1/payments/{attemptReference}`; se mudou,
  atualizar a spec antes do Task 3.
- Executar em branch/worktree isolada (`git worktree`), nunca direto em `main`; abrir uma
  unica PR ao final e aguardar revisao/merge humano — nunca fazer merge sem confirmacao
  explicita do usuario.
- `payment` continua sem importar `order`, `cart`, `customer`, `notification` ou
  `integration`. `integration/checkout` continua sem importar tipos de dominio de `payment`
  ou `order` fora de ports/ACL.
- `LoggingPaymentProviderGateway` continua sendo o default (`enabled=false`/`matchIfMissing = true`);
  nenhum teste existente deve precisar de rede ou do nexus-payment-service real.
- Migrations Flyway portaveis entre H2 e PostgreSQL, sem tipos/funcoes exclusivos de
  PostgreSQL.

---

### Task 1: Modelo — `providerAttemptReference` e persistencia

**Files:**

- Modify: `payment/domain/PaymentAttempt.kt` (+`providerAttemptReference`, +`recordProviderDispatch`)
- Modify: `payment/domain/PaymentProvider.kt` (+`NEXUS_PAYMENT_SERVICE`)
- Create: migration Flyway `V<n>__add_provider_attempt_reference_to_payment_attempts.sql`
- Modify: `payment/adapter/outbound/jpa/PaymentAttemptEntity.kt`,
  `SpringDataPaymentAttemptRepository.kt`, `PaymentJpaRepositoryAdapter.kt`
- Modify: `payment/application/port/outbound/PaymentAttemptRepositoryPort.kt`
  (+`recordProviderDispatch`, +`findPendingByProvider`)
- Test: `payment/domain/PaymentAttemptTest.kt`, teste JPA do adapter

- [ ] **Step 1: Escrever testes vermelhos.** `recordProviderDispatch` so aceita attempt
  `REQUESTED` e mantem status `REQUESTED`; chamar em attempt ja terminal lanca
  `PaymentDomainValidationException`. `findPendingByProvider` so traz `status = REQUESTED` do
  provider pedido. `recordProviderDispatch` do repositorio persiste o campo sem alterar
  status/lease.
- [ ] **Step 2: Rodar os testes.** Esperado: falha por metodo/coluna inexistentes.
- [ ] **Step 3: Implementar.** Migration portavel adicionando coluna nullable
  `provider_attempt_reference`; `@Query` JPQL explicito para `findPendingByProvider` (sem
  derived query).
- [ ] **Step 4: Rodar testes e commit.**

```bash
git add src/main/resources/db/migration src/main src/test
git commit -m "feat: track provider attempt reference on payment attempts"
```

### Task 2: Gateway — `checkStatus` e `NexusPaymentServiceProviderGateway`

**Files:**

- Modify: `payment/application/port/outbound/PaymentProviderGateway.kt` (+`checkStatus`,
  soltar invariante de `ProviderProcessingResult`, +`providerAttemptReference` no result,
  +`ProviderStatusResult`)
- Modify: `payment/adapter/outbound/provider/LoggingPaymentProviderGateway.kt`
  (`@ConditionalOnProperty`, `checkStatus` lanca `UnsupportedOperationException`)
- Create: `payment/adapter/outbound/provider/NexusPaymentServiceProviderGateway.kt`
- Create: `payment/application/exception/PaymentProviderGatewayException.kt`
- Modify: `src/main/resources/application.yml` (bloco `nexus.payment-service`)
- Test: `NexusPaymentServiceProviderGatewayTest.kt` (via `MockRestServiceServer`)

**Interfaces:**

```kotlin
interface PaymentProviderGateway {
    fun process(request: ProviderProcessingRequest): ProviderProcessingResult
    fun checkStatus(providerAttemptReference: String): ProviderStatusResult
}
data class ProviderStatusResult(val status: PaymentStatus, val providerTransactionId: String?)
```

- [ ] **Step 1: Escrever testes vermelhos.** `process()` envia `Idempotency-Key` derivado e
  corpo `{referenceId, amount, paymentToken}` (sem `currency`); resposta `202 PROCESSING`
  mapeia para `ProviderProcessingResult(REQUESTED, providerAttemptReference=<attemptReference
  do corpo>, providerTransactionId=null)`. `checkStatus()` mapeia `PROCESSING` para
  `PaymentStatus` nao-terminal e `APPROVED`/`REJECTED` para terminal. Erros HTTP 4xx/5xx
  viram `PaymentProviderGatewayException` preservando o `code` do corpo.
- [ ] **Step 2: Rodar os testes.** Esperado: falha por classes inexistentes.
- [ ] **Step 3: Implementar com `RestClient`.** Beans condicionais
  (`nexus.payment-service.enabled=true|false/matchIfMissing`) garantindo que Logging e
  NexusPaymentService nunca coexistem.
- [ ] **Step 4: Rodar testes e commit.**

```bash
git add src/main src/test src/main/resources/application.yml
git commit -m "feat: add nexus-payment-service provider gateway"
```

### Task 3: Reconciliacao no contexto Payment

**Files:**

- Modify: `payment/application/usecase/ProcessPaymentUseCase.kt` (`processReservedAttempt`
  usa `recordProviderDispatch` quando `REQUESTED`)
- Create: `payment/application/port/inbound/ReconcilePendingPaymentAttemptsInputPort.kt`
- Create: `payment/application/usecase/ReconcilePendingPaymentAttemptsUseCase.kt`
- Test: `ReconcilePendingPaymentAttemptsUseCaseTest.kt`, teste adicional de `ProcessPaymentUseCaseTest.kt`

- [ ] **Step 1: Escrever testes vermelhos.** Gateway retornando `REQUESTED` mantem attempt
  `REQUESTED` com `providerAttemptReference` gravado, sem chamar `complete()`. Reconciliacao:
  attempts com `checkStatus` ainda `PROCESSING` nao aparecem no resultado; attempts terminais
  completam localmente e aparecem uma unica vez; uma segunda chamada de `reconcile()` nao
  reprocessa quem ja completou.
- [ ] **Step 2: Rodar os testes.** Esperado: falha por metodo/classe inexistentes.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar testes e commit.**

```bash
git add src/main src/test
git commit -m "feat: reconcile pending payment attempts against provider status"
```

### Task 4: Orquestracao no checkout — ACL, use case e scheduler

**Files:**

- Modify: `integration/checkout/application/port/outbound/OrderPaymentResultGateway.kt`
  (+`applyByOrderReference`)
- Modify: `integration/checkout/adapter/outbound/acl/OrderPaymentResultGatewayAdapter.kt`
- Modify: `integration/checkout/adapter/outbound/acl/OrderCreationGatewayAdapter.kt` (helper
  inverso de parse `"checkout:$orderId"`, ao lado de onde o formato e cunhado)
- Modify: `integration/checkout/application/model/CheckoutModels.kt` (novos commands/results)
- Create: `integration/checkout/application/port/outbound/PaymentReconciliationGateway.kt`
- Create: `integration/checkout/adapter/outbound/acl/PaymentReconciliationGatewayAdapter.kt`
- Create: `integration/checkout/application/PaymentReconciliationUseCase.kt`
- Create: `integration/checkout/adapter/inbound/scheduler/PaymentReconciliationScheduler.kt`
- Modify: `PackageStructureArchitectureTest.kt` / `OrderCheckoutBoundaryTest.kt` se necessario
  para cobrir o novo pacote `adapter/inbound/scheduler`
- Test: `PaymentReconciliationUseCaseTest.kt`, teste de arquitetura

- [ ] **Step 1: Escrever testes vermelhos.** `applyByOrderReference` resolve `orderId` a
  partir de `"checkout:$orderId"` e aplica o resultado via
  `ApplyOrderPaymentResultInputPort` existente, indicando se houve transicao real.
  `PaymentReconciliationUseCase`: resultado `APPROVED` confirma o pedido e dispara
  notificacao uma unica vez; `REJECTED` marca `PAYMENT_FAILED` sem notificacao; um
  `referenceId` sem pedido correspondente nao interrompe o processamento do lote. Teste de
  arquitetura cobre que o scheduler/ACL novos nao importam tipos de dominio de
  `payment`/`order` diretamente.
- [ ] **Step 2: Rodar os testes.** Esperado: falha por classes/metodos inexistentes.
- [ ] **Step 3: Implementar.** Scheduler condicional a `nexus.payment-service.enabled=true`,
  intervalo configuravel via `nexus.payment-service.polling-interval`.
- [ ] **Step 4: Rodar testes e commit.**

```bash
git add src/main src/test
git commit -m "feat: reconcile payment provider results into orders via polling"
```

### Task 5: Verificacao end-to-end e fechamento

**Files:**

- Modify: `docker-compose.yml` (opcional: servico `nexus-payment-service` apontando para
  imagem/build context do repo irmao, para teste manual local)
- Nenhuma mudanca de codigo adicional esperada; esta tarefa e de verificacao.

- [ ] **Step 1: Build completo.**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

- [ ] **Step 2: Verificacao manual com nexus-payment-service + dummy-pay reais.** Subir os
  dois via docker-compose deles; subir nexus-shopping com
  `NEXUS_PAYMENT_SERVICE_ENABLED=true` e `NEXUS_PAYMENT_SERVICE_BASE_URL` apontando para a
  instancia real. `POST /checkout` com `paymentToken=card_processing_approved`: resposta
  imediata com pedido `WAITING_PAYMENT`; dentro de `DUMMYPAY_PROCESSING_DELAY` + alguns
  ciclos de `polling-interval`, o pedido muda sozinho para `CONFIRMED` com notificacao unica.
  Repetir com `card_declined` → `PAYMENT_FAILED` sem notificacao.
- [ ] **Step 3: Confirmar nao-reprocessamento.** Apos um attempt virar terminal, ciclos
  seguintes do scheduler nao geram segunda notificacao nem chamada redundante de
  `applyByOrderReference` alem da idempotencia ja garantida por `Order.applyPaymentResult`.
- [ ] **Step 4: Push e PR.**

```bash
git push origin <branch>
```

**Stop condition:** Abrir PR, aguardar revisao e merge humano. Nao fazer merge sem
confirmacao explicita do usuario para esse merge especifico.
