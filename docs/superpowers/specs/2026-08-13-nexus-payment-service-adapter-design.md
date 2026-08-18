# Spec: Adapter outbound para nexus-payment-service via polling

**Status:** Proposta — assume polling; o contrato final de consulta de status no
nexus-payment-service ainda esta sendo redesenhado em sessao paralela (ver "Nota sobre
dependencia externa" abaixo).
**Data:** 2026-08-13
**Pre-requisito:** `2026-07-26-payment-context-design.md`, `2026-07-26-checkout-integration-boundaries-design.md`

## Objetivo

Substituir, de forma opcional e configuravel, o `LoggingPaymentProviderGateway` (provider
simulado local) por um adapter real que fala HTTP com o microservice `nexus-payment-service`
(repo irmao `/Users/fabiano/Developer/nexus-payment-service`, ja implementado e V1-completo,
integrado por sua vez com o simulador `dummy-pay`). Os dois gateways coexistem atras de um
toggle de configuracao: `LOGGING_PROVIDER` continua sendo o default para producao local
simples e para os testes H2 (offline, deterministico); `NEXUS_PAYMENT_SERVICE` e habilitado
explicitamente para exercitar a integracao real via docker-compose.

## Nota sobre dependencia externa

O nexus-payment-service despacha pagamentos de forma assincrona: `POST /v1/payments`
sempre responde `202 PROCESSING` — nunca um resultado terminal na criacao. O design
original prevé confirmar o resultado por webhook assinado (`X-Webhook-Signature`,
HMAC-SHA256) postado em `NEXUS_PAYMENT_CALLBACK_URL`. Esse design esta sendo revisado em
outra sessao para abandonar o webhook em favor de o nexus-shopping fazer **polling** do
status do pagamento; essa revisao ainda nao fechou o contrato final (endpoint dedicado vs.
o `GET /v1/payments/{attemptReference}` ja existente hoje, formato de resposta, cadencia
recomendada). Esta spec assume polling e usa o contrato **hoje documentado e implementado**
como alvo. Se o contrato mudar, o unico ponto de impacto e
`NexusPaymentServiceProviderGateway.checkStatus` (secao "Gateway do provider" abaixo) — o
resto do desenho (reconciliacao, aplicacao ao pedido, notificacao) independe do transporte
exato usado para descobrir o status.

## Situacao atual reaproveitada

O bounded context `payment` (`2026-07-26-payment-context-design.md`) ja foi desenhado
prevendo um resultado nao-terminal: `PaymentStatus` inclui `REQUESTED`, `OrderStatus` ja tem
`PAYMENT_PROCESSING`, e `ExecuteCheckoutUseCase.execute()` ja tem o desvio
`if (payment.status == PaymentResultStatus.REQUESTED) return order`, hoje um branch morto
porque nenhum gateway real o alcanca. Este refactor ativa esse caminho e adiciona a peca que
faltava: quem descobre, mais tarde, que um pagamento virou terminal, e aplica esse resultado
ao pedido.

## Despacho do pagamento

- `POST {baseUrl}/v1/payments`, header `Idempotency-Key: <providerDispatchKey>` — reaproveita
  sem alteracao o `providerDispatchKey` ja calculado por `PaymentProviderDispatchKey.current(...)`
  em `ProcessPaymentUseCase.kt`.
- Corpo: `{ "referenceId": ..., "amount": <cents Long>, "paymentToken": ... }` — sem campo de
  moeda (BRL e implicito no nexus-payment-service).
- Resposta sempre `202 { attemptReference, referenceId, status: "PROCESSING", replayed }`. O
  `attemptReference` desta resposta pertence ao **nexus-payment-service** e e distinto do
  `attemptReference` que o nexus-shopping ja gera localmente em `ProcessPaymentUseCase.process()`
  — os dois precisam ser guardados separadamente, pois o identificador remoto e o que sera
  usado depois para consultar status.
- Erros (`400/409/422/500`) mapeiam para `PaymentProviderGatewayException` nova, propagada
  para cima: o checkout falha com 5xx e o pedido fica em `WAITING_PAYMENT` sem attempt
  despachado. Retry desse caso fica fora de escopo (ver "Fora de escopo").

## Consulta de status (polling)

- `GET {baseUrl}/v1/payments/{providerAttemptReference}` → `{ attemptReference, status }`
  (`PROCESSING`, `APPROVED` ou `REJECTED`). Enquanto `PROCESSING`, nada muda; ao virar
  terminal, o resultado deve ser aplicado ao `PaymentAttempt` local e, por consequencia, ao
  `Order`.
- Quem decide quando consultar e o nexus-shopping: um scheduler local varre periodicamente
  os `PaymentAttempt` locais ainda `REQUESTED` com `provider = NEXUS_PAYMENT_SERVICE` e
  consulta cada um.

## Gateway do provider

`PaymentProviderGateway` (`payment/application/port/outbound`) ganha um segundo metodo:

```text
PaymentProviderGateway
  process(referenceId, amount, currency, paymentToken, providerDispatchKey) -> ProviderProcessingResult
  checkStatus(providerAttemptReference) -> ProviderStatusResult
```

`ProviderProcessingResult` deixa de exigir status terminal: um resultado `REQUESTED` passa a
ser valido, significando "despachado, aguardando confirmacao assincrona"; nesse caso o novo
campo `providerAttemptReference` deve vir preenchido. `ProviderStatusResult(status,
providerTransactionId)` reaproveita `PaymentStatus` (terminal ou `REQUESTED`/"ainda
processando").

`NexusPaymentServiceProviderGateway`, novo, em `payment/adapter/outbound/provider`, implementa
os dois metodos usando `org.springframework.web.client.RestClient` (ja disponivel via
`spring-boot-starter-web` no Spring Boot 4 — nenhuma dependencia nova no Gradle). E um
`@Component` condicional: `@ConditionalOnProperty(prefix = "nexus.payment-service", name =
["enabled"], havingValue = "true")`. `LoggingPaymentProviderGateway` ganha a condicao
inversa (`havingValue = "false", matchIfMissing = true`) para os dois nunca coexistirem como
beans do mesmo tipo; seu `checkStatus(...)` lanca `UnsupportedOperationException` (nunca
deveria ser chamado, pois o reconciliador so busca attempts com `provider =
NEXUS_PAYMENT_SERVICE`).

Erros HTTP (`RestClientResponseException`) mapeiam para `PaymentProviderGatewayException`
nova, preservando o `code` do corpo de erro do nexus-payment-service na mensagem, para
depuracao.

## Modelo

`PaymentAttempt` ganha um campo novo, `providerAttemptReference: String?` (identificador
remoto do nexus-payment-service, distinto do `attemptReference` local), e um metodo de
dominio espelhando `complete()`:

```kotlin
fun recordProviderDispatch(providerAttemptReference: String): PaymentAttempt {
    if (status != PaymentStatus.REQUESTED) {
        throw PaymentDomainValidationException("only requested payment attempts can record a provider dispatch.")
    }
    return copy(providerAttemptReference = providerAttemptReference)
}
```

Uma migration Flyway portavel adiciona a coluna `provider_attempt_reference` (nullable) em
`payment_attempts`. `PaymentProvider` ganha o valor `NEXUS_PAYMENT_SERVICE`.

`PaymentAttemptRepositoryPort` ganha dois metodos:

- `recordProviderDispatch(attemptReference, processingLeaseToken, providerAttemptReference) -> PaymentAttempt?` —
  persiste o identificador remoto mantendo o status `REQUESTED`.
- `findPendingByProvider(provider, limit = 100) -> List<PaymentAttempt>` — attempts com
  `status = REQUESTED` e `provider` dado, via `@Query` JPQL explicito (sem derived query,
  seguindo convencao do projeto).

`ProcessPaymentUseCase.processReservedAttempt`: quando `providerResult.status ==
PaymentStatus.REQUESTED`, chama `recordProviderDispatch(...)` em vez de `complete(...)`;
quando terminal, o fluxo atual permanece inalterado.

## Reconciliacao (payment)

Novo inbound port `ReconcilePendingPaymentAttemptsInputPort`:

```kotlin
interface ReconcilePendingPaymentAttemptsInputPort {
    fun reconcile(): List<PaymentReconciliationResult>
}
data class PaymentReconciliationResult(
    val attemptReference: String,
    val referenceId: String,
    val status: PaymentStatus, // sempre terminal aqui
    val providerTransactionId: String?,
)
```

`ReconcilePendingPaymentAttemptsUseCase` busca `findPendingByProvider(NEXUS_PAYMENT_SERVICE)`;
para cada attempt chama `paymentProviderGateway.checkStatus(providerAttemptReference)`; se
ainda `PROCESSING`, ignora; se terminal, chama `paymentAttemptRepository.complete(...)`
(metodo ja existente, sem mudancas) e inclui no resultado retornado. So devolve os que
**acabaram de** virar terminal nesta chamada — e o que o orquestrador do checkout aplica ao
pedido. Como `findPendingByProvider` so traz `status = REQUESTED`, um attempt ja completado
nao aparece de novo em uma proxima chamada — a idempotencia da reconciliacao vem gratis
dessa consulta.

## Integracao do checkout

A correlacao `referenceId ("checkout:$orderId") → orderId` e a orquestracao de duas
escritas (attempt ja resolvido pelo `payment`, mais `Order` e notificacao) permanecem
responsabilidade de `checkout`, pela mesma razao de ser da ACL que ja existe
para o caminho sincrono: nem `payment` nem `order` devem se conhecer.

- Novo outbound port `PaymentReconciliationGateway` (`fun reconcile(): List<PaymentReconciliationOutcome>`),
  com ACL adapter `PaymentReconciliationGatewayAdapter` embrulhando
  `ReconcilePendingPaymentAttemptsInputPort`.
- `OrderPaymentResultGateway` ganha `applyByOrderReference(command: ApplyOrderPaymentResultByReferenceCommand):
  AppliedOrderPaymentResult` — recebe `orderReference: String` em vez de `CheckoutOrderSnapshot`
  completo, porque o reconciliador so tem o `referenceId`. O ACL adapter faz o parse
  `"checkout:$orderId"` (mesmo formato que `OrderCreationGatewayAdapter.toCheckoutSnapshot`
  ja cunha — a funcao inversa fica no mesmo pacote `acl`, onde o formato e definido) e chama
  `ApplyOrderPaymentResultInputPort.apply(...)` como hoje. `AppliedOrderPaymentResult` indica
  se houve transicao de fato, para decidir se dispara notificacao.
- Novo use case `PaymentReconciliationUseCase`: chama `PaymentReconciliationGateway.reconcile()`;
  para cada resultado, chama `OrderPaymentResultGateway.applyByOrderReference(...)` e, se
  houve transicao real para `APPROVED`, dispara `NotificationGateway.ensureOrderConfirmation(...)` —
  espelha o fim de `ExecuteCheckoutUseCase.execute()`, mantendo paridade de comportamento
  entre o caminho sincrono (Logging) e o assincrono (nexus-payment-service).
- Novo scheduler `PaymentReconciliationScheduler`
  (`checkout/adapter/inbound/scheduler`): `@Scheduled(fixedDelayString =
  "${nexus.payment-service.polling-interval:2000}")`, condicional ao mesmo
  `nexus.payment-service.enabled=true`, chama `PaymentReconciliationUseCase.reconcile()` a
  cada tick. Fica em `adapter/inbound` porque, em termos hexagonais, e um mecanismo que
  dirige a aplicacao — mesma posicao que um webhook receiver ocuparia, so que disparado por
  um timer em vez de uma requisicao.

## Configuracao

```yaml
nexus:
  payment-service:
    enabled: ${NEXUS_PAYMENT_SERVICE_ENABLED:false}
    base-url: ${NEXUS_PAYMENT_SERVICE_BASE_URL:http://localhost:8081}
    polling-interval: ${NEXUS_PAYMENT_SERVICE_POLLING_INTERVAL:2000}
```

Segue o padrao ja usado por `nexus.payment.authorization-fingerprint-secret`. Nada muda em
`src/test/resources/application.yml`: o default `enabled=false` (via `matchIfMissing = true`
no Logging) garante que os testes continuam offline e deterministicos sem qualquer alteracao.

## Idempotencia e concorrencia

- Um attempt so entra em `findPendingByProvider` enquanto `status = REQUESTED`; uma vez
  completado, some da lista — a reconciliacao repetida nao reprocessa.
- `docker-compose.yml` sobe 3 instancias da aplicacao (`app1/app2/app3`) atras do nginx; com
  o scheduler rodando nas 3, chamadas de `GET` podem ser duplicadas entre instancias (barato,
  apenas leitura) — a seguranca fica no `complete()` do repositorio, que precisa ser uma
  atualizacao condicionada a `status = REQUESTED` (mesma garantia que `PaymentAttempt.complete()`
  ja exige no dominio). Confirmar durante a implementacao que
  `PaymentJpaRepositoryAdapter.complete()` de fato faz essa atualizacao condicional — se nao
  fizer, e a mesma race que ja existiria hoje entre requests concorrentes, correcao e nao
  regressao introduzida por este refactor. Uma unica instancia lider ou `SELECT ... FOR
  UPDATE SKIP LOCKED` (como o proprio dummy-pay faz no seu outbox worker) fica como
  otimizacao futura, fora de escopo aqui.
- `Order.applyPaymentResult` ja e idempotente por `attemptReference` — nenhuma mudanca
  necessaria la.

## Testes de aceitacao

1. `NexusPaymentServiceProviderGateway`: `process()` envia `Idempotency-Key` e corpo
   corretos e mapeia `202 PROCESSING` para `ProviderProcessingResult(REQUESTED,
   providerAttemptReference=..., providerTransactionId=null)`; `checkStatus()` mapeia
   `PROCESSING`/`APPROVED`/`REJECTED`; erros 4xx/5xx viram `PaymentProviderGatewayException`.
2. `ProcessPaymentUseCase`: gateway retornando `REQUESTED` mantem o attempt `REQUESTED` com
   `providerAttemptReference` gravado; `complete()` do repositorio nao e chamado.
3. `ReconcilePendingPaymentAttemptsUseCase`: attempts pendentes viram terminais so quando o
   gateway reporta status terminal; attempts que continuam `PROCESSING` nao geram resultado;
   chamada repetida nao reprocessa um attempt ja completado.
4. `PaymentReconciliationUseCase`: pedido muda para `CONFIRMED`/`PAYMENT_FAILED` conforme o
   resultado; notificacao dispara so uma vez por pedido; um `referenceId` sem pedido
   correspondente nao derruba o processamento dos demais itens do lote.
5. Teste de arquitetura confirma que `payment` continua sem importar `order`/`integration`, e
   que o novo scheduler/ACL de `checkout` nao importa tipos de dominio de
   `payment`/`order` diretamente (so via ports/ACL, como hoje).
6. Testes HTTP existentes do checkout (`PaymentCheckoutHttpTest.kt` e afins) continuam verdes
   sem alteracao, com `nexus.payment-service.enabled=false` implicito.

## Fora de escopo

- Retry quando a chamada de despacho (`POST /v1/payments`) falha por rede/timeout — o attempt
  local fica em `REQUESTED` sem `providerAttemptReference` e por isso nunca entra em
  `findPendingByProvider` (que exige o attempt ja despachado); fica preso permanentemente sem
  essa peca. Mesmo caso ja sinalizado no roadmap da ADR de contexto Payment como fase futura
  ("demonstrar timeout/retry/idempotencia").
- Contrato final de polling do nexus-payment-service (endpoint dedicado vs. `GET
  /v1/payments/{attemptReference}` atual, formato de resposta, cadencia recomendada) — depende
  do fechamento da sessao paralela; o impacto de uma mudanca fica isolado em
  `NexusPaymentServiceProviderGateway.checkStatus`.
- Qualquer alteracao no repositorio `nexus-payment-service` em si (repo irmao, fora deste
  repositorio e desta spec).
- Lider unico/`SELECT ... FOR UPDATE SKIP LOCKED` para o scheduler — redundancia de leitura
  entre instancias e aceita por ora.
- Configuracao de docker-compose apontando as duas aplicacoes uma para a outra (documentado
  como passo manual/local, nao automatizado nesta entrega).

## Criterio de conclusao

Com `nexus.payment-service.enabled=true` e o nexus-payment-service + dummy-pay rodando, um
checkout com token de aprovacao termina com o pedido em `CONFIRMED` e notificacao unica,
sem qualquer chamada sincrona bloqueante alem do despacho inicial; um checkout com token de
recusa termina em `PAYMENT_FAILED` sem notificacao. Com `enabled=false` (default), o
comportamento observavel do checkout permanece identico ao atual (Logging Gateway,
sincrono), e a suite de testes existente passa sem alteracao.
