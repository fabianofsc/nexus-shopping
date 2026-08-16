# Plano de Implementacao: Integracao HTTP do Notification Service

> **Para agentes:** SUB-SKILL OBRIGATORIA: use `superpowers:subagent-driven-development` (recomendado) ou `superpowers:executing-plans` para executar este plano tarefa a tarefa. Os passos usam checkboxes (`- [ ]`) para acompanhamento.

**Objetivo:** Substituir o bounded context local `notification` por um journal duravel no Checkout, uma chamada HTTP sincrona de aceite ao Notification Service e recuperacao manual pelo backoffice.

**Arquitetura:** `integration/checkout` e dono da intencao imutavel de submeter uma confirmacao de pedido. Aprovacao de Order e reserva de `NotificationSubmission` ocorrem na mesma transacao local; depois do commit, uma ACL HTTP tenta o aceite remoto. Notification Service continua como dono exclusivo da notificacao remota e de sua entrega.

**Tecnologias:** Kotlin, Java 21, Spring Boot 4, Spring Data JPA, Flyway, H2, PostgreSQL, `RestClient`, MockRestServiceServer, WireMock e Gradle Wrapper.

## Restricoes globais

- Respeitar `adapter -> application -> domain`; `domain/` e `application/` nao importam JPA, Hibernate ou Spring Data.
- DTO HTTP converte em command com `toCommand()`; JPA converte com `toEntity()` e `toDomain()`.
- Toda consulta Spring Data usa `@Query` JPQL explicita; paginacao le `size + 1`, sem `COUNT(*)`.
- Nenhuma chamada HTTP ocorre dentro de transacao de Cart, Order, Payment ou journal.
- Indisponibilidade do Notification Service nao altera a resposta bem-sucedida do checkout; registra erro sanitizado sem destinatario, assunto, corpo ou senha em logs.
- Retry reutiliza literalmente payload e `Idempotency-Key` persistidos; nao renderiza de novo.
- Arquivos novos ou editados de documentacao ficam em portugues e ASCII.
- Usar Gradle somente como `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ...`.

---

## Estrutura de arquivos

| Caminho | Responsabilidade |
|---|---|
| `integration/checkout/application/model/NotificationSubmission.kt` | Estado e payload imutavel, sem framework. |
| `integration/checkout/application/port/inbound/NotificationSubmissionBackofficeInputPort.kt` | Contrato do backoffice. |
| `integration/checkout/application/port/outbound/NotificationSubmissionRepositoryPort.kt` | Persistencia, lease e pagina do journal. |
| `integration/checkout/application/port/outbound/NotificationServiceClientPort.kt` | Seam pequena para aceitar uma submissao remota. |
| `integration/checkout/application/usecase/NotificationSubmissionUseCase.kt` | Implementa `NotificationGateway` e o contrato do backoffice. |
| `integration/checkout/adapter/outbound/jpa/*NotificationSubmission*` | Entity, JPQL repository e adapter JPA. |
| `integration/checkout/adapter/outbound/notification/NotificationServiceHttpClient.kt` | ACL HTTP, Basic Auth e DTOs privados. |
| `integration/checkout/adapter/outbound/notification/NotificationSubmissionConfiguration.kt` | Composicao Spring do use case puro. |
| `integration/checkout/adapter/inbound/http/backoffice/*` | Controller e DTOs do backoffice. |
| `V11__replace_local_notification_context_with_submission_journal.sql` | Cria journal e remove a tabela local sem consumidores. |

### Tarefa 1: Definir modelo e portas sem framework

**Arquivos:**

- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/application/model/NotificationSubmission.kt`
- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/application/port/inbound/NotificationSubmissionBackofficeInputPort.kt`
- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/application/port/outbound/NotificationSubmissionRepositoryPort.kt`
- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/application/port/outbound/NotificationServiceClientPort.kt`
- Modificar: `src/main/kotlin/com/nexus/shopping/integration/checkout/application/port/outbound/NotificationGateway.kt`
- Testar: `src/test/kotlin/com/nexus/shopping/integration/checkout/NotificationSubmissionModelTest.kt`

**Interfaces:**

- Produz `NotificationSubmission`, `NotificationSubmissionStatus` e `NotificationSubmissionSummary`.
- `NotificationGateway` passa a expor `reserveOrderConfirmation(command)` e `dispatch(submissionId)`.
- O backoffice expoe `list(status, page, size)`, `retry(submissionId)` e `discard(command)`.

- [ ] **Passo 1: Escrever os testes unitarios que falham**

```kotlin
@Test
fun `cria payload estavel para confirmacao do pedido`() {
    val submission = NotificationSubmission.forOrderConfirmation(command)

    assertEquals("order-confirmed:42:attempt-1", submission.notificationKey)
    assertEquals("order:42", submission.referenceId)
    assertEquals(NotificationSubmissionStatus.PENDING, submission.status)
}

@Test
fun `descarte e terminal e exige justificativa`() {
    assertFailsWith<CheckoutValidationException> { pending.discard(" ") }
    assertEquals(NotificationSubmissionStatus.DISCARDED, pending.discard("template remoto invalido").status)
}
```

- [ ] **Passo 2: Executar para confirmar a falha**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationSubmissionModelTest'`

Esperado: falha de compilacao, pois `NotificationSubmission` ainda nao existe.

- [ ] **Passo 3: Implementar o modelo e contratos exatos**

```kotlin
enum class NotificationSubmissionStatus { PENDING, IN_FLIGHT, ACCEPTED, FAILED, DISCARDED }

interface NotificationGateway {
    fun reserveOrderConfirmation(command: EnsureOrderConfirmationCommand): NotificationSubmission
    fun dispatch(submissionId: Long): NotificationSubmission
}

interface NotificationSubmissionBackofficeInputPort {
    fun list(status: NotificationSubmissionStatus?, page: Int, size: Int): PageResult<NotificationSubmissionSummary>
    fun retry(submissionId: Long): NotificationSubmissionSummary
    fun discard(command: DiscardNotificationSubmissionCommand): NotificationSubmissionSummary
}
```

`NotificationServiceClientPort.accept(submission)` retorna somente
`AcceptedNotification(notificationId)` e declara
`NotificationServiceUnavailableException` e
`NotificationServiceRejectedException` como erros tipados. Definir tambem
`DiscardNotificationSubmissionCommand(submissionId, reason)`. O repository port
declara `reserve`, `findById`, `claim`, `markAccepted`, `markFailed`, `discard` e
`findPage`; os metodos de lease recebem o token explicitamente. O modelo cuna chave
`order-confirmed:{orderId}:{attemptReference}`, referencia `order:{orderId}`,
assunto e corpo de confirmacao ja renderizados.

- [ ] **Passo 4: Executar o teste para confirmar sucesso**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationSubmissionModelTest'`

Esperado: PASS.

- [ ] **Passo 5: Commitar modelo e portas**

```bash
rtk git add src/main/kotlin/com/nexus/shopping/integration/checkout/application src/test/kotlin/com/nexus/shopping/integration/checkout/NotificationSubmissionModelTest.kt
rtk git commit -m "feat: define notification submission journal"
```

### Tarefa 2: Criar migration e adapter JPA do journal

**Arquivos:**

- Criar: `src/main/resources/db/migration/V11__replace_local_notification_context_with_submission_journal.sql`
- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/adapter/outbound/jpa/NotificationSubmissionEntity.kt`
- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/adapter/outbound/jpa/SpringDataNotificationSubmissionRepository.kt`
- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/adapter/outbound/jpa/NotificationSubmissionJpaRepositoryAdapter.kt`
- Testar: `src/test/kotlin/com/nexus/shopping/integration/checkout/NotificationSubmissionMigrationContractTest.kt`
- Testar: `src/test/kotlin/com/nexus/shopping/integration/checkout/adapter/outbound/jpa/NotificationSubmissionJpaRepositoryAdapterTest.kt`

**Interfaces:**

- Consome `NotificationSubmissionRepositoryPort` da Tarefa 1.
- Produz reserva idempotente, claim por lease, conclusao condicional, descarte e `PageResult` portaveis entre H2 e PostgreSQL.

- [ ] **Passo 1: Escrever testes de migration e lease que falham**

```kotlin
@Test
fun `migration remove notifications legado e cria journal com constraints`() {
    flyway.migrate()
    assertEquals(0, countRows(connection, "INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'NOTIFICATIONS'"))
    assertEquals(1, countRows(connection, "INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'NOTIFICATION_SUBMISSIONS'"))
}

@Test
fun `claim e conclusao exigem o token atual da lease`() {
    assertNotNull(adapter.claim(id, "lease-a", now.plusSeconds(30), now))
    assertNull(adapter.markAccepted(id, "lease-b", "ntf_remote", now))
    assertEquals(NotificationSubmissionStatus.ACCEPTED, adapter.markAccepted(id, "lease-a", "ntf_remote", now)?.status)
}
```

- [ ] **Passo 2: Executar os testes para confirmar a falha**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationSubmissionMigrationContractTest' --tests '*NotificationSubmissionJpaRepositoryAdapterTest'`

Esperado: migration e classes ausentes.

- [ ] **Passo 3: Implementar schema portavel e JPQL explicito**

Criar `notification_submissions` antes de `DROP TABLE notifications`. A tabela tem
`notification_key` unico, `status`, `order_id`, `attempt_reference`, payload
imutavel, `attempt_count`, `last_error`, `notification_id`, lease e timestamps.
Criar indice por `status, created_at`; nao criar FK para Order, pois o journal e
tecnico e a correlacao pelo ID e suficiente.

O claim aceita somente `PENDING`, `FAILED` ou `IN_FLIGHT` com `leaseUntil < now`,
incrementa tentativa e grava token/deadline. `markAccepted` e `markFailed` exigem
ID e token, limpam lease e gravam resultado. `findPage` usa `PageRequest.of(page,
size + 1)`, ordenacao `createdAt ASC, id ASC` e converte para `PageResult`, sem
consulta de contagem.

- [ ] **Passo 4: Executar os testes para confirmar sucesso**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationSubmissionMigrationContractTest' --tests '*NotificationSubmissionJpaRepositoryAdapterTest'`

Esperado: PASS em H2.

- [ ] **Passo 5: Commitar migration e persistencia**

```bash
rtk git add src/main/resources/db/migration/V11__replace_local_notification_context_with_submission_journal.sql
rtk git commit -m "db: add notification submission journal"
rtk git add src/main/kotlin/com/nexus/shopping/integration/checkout/adapter/outbound/jpa src/test/kotlin/com/nexus/shopping/integration/checkout
rtk git commit -m "feat: persist notification submissions"
```

### Tarefa 3: Implementar orquestracao de dispatch e recuperacao manual

**Arquivos:**

- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/application/usecase/NotificationSubmissionUseCase.kt`
- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/adapter/outbound/notification/NotificationSubmissionConfiguration.kt`
- Testar: `src/test/kotlin/com/nexus/shopping/integration/checkout/NotificationSubmissionUseCaseTest.kt`

**Interfaces:**

- Consome os repository/client ports das Tarefas 1 e 2.
- Produz o bean que implementa `NotificationGateway` e `NotificationSubmissionBackofficeInputPort`.

- [ ] **Passo 1: Escrever testes do use case com fakes manuais**

```kotlin
@Test
fun `dispatch registra falha remota e retorna normalmente`() {
    client.failure = NotificationServiceUnavailableException("timeout")

    val result = useCase.dispatch(pending.id!!)

    assertEquals(NotificationSubmissionStatus.FAILED, result.status)
    assertEquals(1, repository.attempts.single().attemptCount)
}

@Test
fun `retry reutiliza chave e payload persistidos`() {
    useCase.retry(failed.id!!)

    assertEquals(failed.notificationKey, client.accepted.single().notificationKey)
    assertEquals(failed.body, client.accepted.single().body)
}
```

- [ ] **Passo 2: Executar para confirmar a falha**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationSubmissionUseCaseTest'`

Esperado: `NotificationSubmissionUseCase` inexistente.

- [ ] **Passo 3: Implementar use case puro e configuracao Spring**

O use case nao importa Spring. `reserveOrderConfirmation` faz reserva idempotente.
`dispatch` reclama submissao com UUID e lease de 30 segundos, chama
`NotificationServiceClientPort.accept` fora de transacao e finaliza condicionalmente
em `ACCEPTED` ou `FAILED`. Falhas esperadas sao sanitizadas e nao escapam para o
Checkout. `retry` valida estado elegivel e chama `dispatch`; `discard(command)`
exige razao de 1..500 caracteres e faz transicao terminal.

`NotificationSubmissionConfiguration` fica no adapter e publica a mesma instancia
do use case como `NotificationGateway` e como
`NotificationSubmissionBackofficeInputPort`.

- [ ] **Passo 4: Executar para confirmar sucesso**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationSubmissionUseCaseTest'`

Esperado: PASS.

- [ ] **Passo 5: Commitar use case e configuracao**

```bash
rtk git add src/main/kotlin/com/nexus/shopping/integration/checkout/application/usecase/NotificationSubmissionUseCase.kt src/main/kotlin/com/nexus/shopping/integration/checkout/adapter/outbound/notification/NotificationSubmissionConfiguration.kt src/test/kotlin/com/nexus/shopping/integration/checkout/NotificationSubmissionUseCaseTest.kt
rtk git commit -m "feat: add manual notification submission recovery"
```

### Tarefa 4: Implementar ACL HTTP do Notification Service

**Arquivos:**

- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/adapter/outbound/notification/NotificationServiceHttpClient.kt`
- Criar: `src/main/kotlin/com/nexus/shopping/infra/http/ConfigurableRestClientFactory.kt`
- Modificar: `src/main/resources/application.yml`
- Testar: `src/test/kotlin/com/nexus/shopping/integration/checkout/adapter/outbound/notification/NotificationServiceHttpClientTest.kt`

**Interfaces:**

- Consome `NotificationServiceClientPort.accept(submission)`.
- Produz `AcceptedNotification(notificationId)` para `202` e excecoes tipadas recuperavel/nao-recuperavel para as demais respostas.

- [ ] **Passo 1: Escrever testes do client com MockRestServiceServer**

```kotlin
server.expect(requestTo("http://notification-service/v1/notifications"))
    .andExpect(header("Authorization", "Basic bm90aWZpY2F0aW9uOm5vdGlmaWNhdGlvbg=="))
    .andExpect(header("Idempotency-Key", "order-confirmed:42:attempt-1"))
    .andExpect(content().json(expectedEmailPayload))
    .andRespond(withStatus(HttpStatus.ACCEPTED).body("""{"notification_id":"ntf_1"}"""))

assertEquals("ntf_1", client.accept(submission).notificationId)
```

Cobrir timeout, `429` e `500` como recuperaveis; `400`, `401`, `403`, `409` e
`422` como nao-recuperaveis; e corpo `202` vazio ou malformado como falha.

- [ ] **Passo 2: Executar para confirmar a falha**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationServiceHttpClientTest'`

Esperado: client ausente.

- [ ] **Passo 3: Implementar ACL e factory configuravel**

`ConfigurableRestClientFactory` fica em `infra/http`, usa JDK `HttpClient` em
HTTP/1.1 e cria um builder com timeouts recebidos. O Notification client recebe
`base-url`, `username`, `password`, `connect-timeout` e `read-timeout` de
`nexus.notification-service`, com defaults `http://notification-service:8080`,
`notification`, `notification`, `5s` e `5s`.

Enviar `POST /v1/notifications` com Basic Auth, `Idempotency-Key`, `EMAIL`,
`recipient.email`, assunto, corpo, `reference_id`, `callback_id` e
`callback_name`. DTOs JSON permanecem privados e usam nomes snake_case.
Logs estruturados incluem operacao, latencia, status/codigo remoto, ID local e ID
remoto; nunca incluem segredo ou dados da mensagem.

- [ ] **Passo 4: Executar testes do client e do gateway de Payment existente**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationServiceHttpClientTest' --tests '*PaymentServiceProviderGatewayTest'`

Esperado: PASS; o novo factory nao muda o gateway de pagamento atual.

- [ ] **Passo 5: Commitar ACL HTTP**

```bash
rtk git add src/main/kotlin/com/nexus/shopping/integration/checkout/adapter/outbound/notification src/main/kotlin/com/nexus/shopping/infra/http/ConfigurableRestClientFactory.kt src/main/resources/application.yml src/test/kotlin/com/nexus/shopping/integration/checkout/adapter/outbound/notification
rtk git commit -m "feat: call notification service over http"
```

### Tarefa 5: Tornar aprovacao e reserva atomicas nos dois caminhos

**Arquivos:**

- Modificar: `src/main/kotlin/com/nexus/shopping/integration/checkout/application/CheckoutWorkflowUseCase.kt`
- Modificar: `src/main/kotlin/com/nexus/shopping/integration/checkout/application/PaymentReconciliationUseCase.kt`
- Modificar: `src/test/kotlin/com/nexus/shopping/integration/checkout/CheckoutWorkflowUseCaseTest.kt`
- Modificar: `src/test/kotlin/com/nexus/shopping/integration/checkout/PaymentReconciliationUseCaseTest.kt`
- Criar: `src/test/kotlin/com/nexus/shopping/integration/checkout/NotificationSubmissionCheckoutIntegrationTest.kt`

**Interfaces:**

- Consome `NotificationGateway` e `TransactionPort`.
- Produz exatamente uma submissao duravel para cada pedido que transicionou a aprovado, no checkout sincrono e na reconciliacao posterior.

- [ ] **Passo 1: Escrever testes de ordem transacional que falham**

```kotlin
@Test
fun `checkout aprovado reserva notificacao na transacao e despacha apos commit`() {
    workflow.execute(approvedCheckout)

    assertEquals(
        listOf("transaction:start", "order:apply", "notification:reserve", "transaction:commit", "notification:dispatch"),
        events,
    )
}

@Test
fun `falha no dispatch nao muda resposta do checkout aprovado`() {
    notificationGateway.dispatchFailure = true

    assertEquals("CONFIRMED", workflow.execute(approvedCheckout).status)
}
```

- [ ] **Passo 2: Executar para confirmar a falha**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*CheckoutWorkflowUseCaseTest' --tests '*PaymentReconciliationUseCaseTest'`

Esperado: a reserva nao existe e a notificacao ainda e chamada diretamente apos aplicar pagamento.

- [ ] **Passo 3: Refatorar os dois orquestradores**

Em `CheckoutWorkflowUseCase`, envolver `orderPaymentResults.apply(...)` e
`notifications.reserveOrderConfirmation(...)` na mesma `transaction.inTransaction`
quando o pagamento for aprovado. Depois do retorno, chamar
`notifications.dispatch(submission.id)` fora do bloco. Manter sem mudanca os
caminhos `REQUESTED` e rejeitado.

Em `PaymentReconciliationUseCase`, injetar `TransactionPort`, aplicar cada
aprovacao e reservar dentro da mesma transacao, depois despachar apos commit.
Continuar isolando erro de um outcome para que o lote prossiga.

- [ ] **Passo 4: Executar testes unitarios e integracao H2**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationSubmissionCheckoutIntegrationTest' --tests '*CheckoutWorkflowUseCaseTest' --tests '*PaymentReconciliationUseCaseTest'`

Esperado: PASS; replay cria uma unica submissao e falha remota deixa Order
`CONFIRMED` com journal `FAILED`.

- [ ] **Passo 5: Commitar orquestracao atomica**

```bash
rtk git add src/main/kotlin/com/nexus/shopping/integration/checkout/application src/test/kotlin/com/nexus/shopping/integration/checkout
rtk git commit -m "feat: journal notification submissions with approved orders"
```

### Tarefa 6: Expor backoffice interno de list/retry/discard

**Arquivos:**

- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/adapter/inbound/http/backoffice/NotificationSubmissionBackofficeController.kt`
- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/adapter/inbound/http/backoffice/dto/NotificationSubmissionBackofficeResponse.kt`
- Criar: `src/main/kotlin/com/nexus/shopping/integration/checkout/adapter/inbound/http/backoffice/dto/DiscardNotificationSubmissionRequest.kt`
- Testar: `src/test/kotlin/com/nexus/shopping/integration/checkout/adapter/inbound/http/backoffice/NotificationSubmissionBackofficeControllerTest.kt`

**Interfaces:**

- Consome `NotificationSubmissionBackofficeInputPort`.
- Produz `GET /backoffice/notification-submissions`, `POST /{id}/retry` e `POST /{id}/discard` com `PageResponse` e RFC 7807.

- [ ] **Passo 1: Escrever testes de controller que falham**

```kotlin
@Test
fun `lista falhas sem destinatario ou corpo`() {
    val response = client.get().uri("/backoffice/notification-submissions?status=FAILED&page=0&size=50").exchange()

    response.expectStatus().isOk
    response.expectBody().jsonPath("$.content[0].lastError").isEqualTo("timeout")
    response.expectBody().jsonPath("$.content[0].recipientEmail").doesNotExist()
}

@Test
fun `descarte exige justificativa e torna submissao terminal`() {
    client.post().uri("/backoffice/notification-submissions/10/discard")
        .bodyValue("""{"reason":"template remoto invalido"}""").exchange()
        .expectStatus().isOk
}
```

- [ ] **Passo 2: Executar para confirmar a falha**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationSubmissionBackofficeControllerTest'`

Esperado: 404 ou erro de compilacao, pois controller ainda nao existe.

- [ ] **Passo 3: Implementar controller e conversao de DTO**

`DiscardNotificationSubmissionRequest.toCommand(submissionId)` constroi o command de
aplicacao. Validar pagina `>= 0`, tamanho `1..500`, enum de status e razao
`1..500` no use case. Estado ineligivel vira `ConflictException`; ID desconhecido
vira `NotFoundException`. A listagem e o retorno de comando mostram somente ID,
pedido, chave, estado, tentativas, ultimo erro, ID remoto e timestamps.

Os endpoints sao internos por topologia de rede/ingress, pois o projeto atual nao
tem Spring Security. Nao adicionar autenticacao nova nesta entrega.

- [ ] **Passo 4: Executar testes HTTP e de Problem Details**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationSubmissionBackofficeControllerTest' --tests '*ApiExceptionHandler*'`

Esperado: PASS, incluindo request malformado, ID inexistente e transicao em conflito.

- [ ] **Passo 5: Commitar adapter do backoffice**

```bash
rtk git add src/main/kotlin/com/nexus/shopping/integration/checkout/adapter/inbound/http/backoffice src/test/kotlin/com/nexus/shopping/integration/checkout/adapter/inbound/http/backoffice
rtk git commit -m "feat: add notification submission backoffice"
```

### Tarefa 7: Remover contexto local e atualizar runtime e documentacao

**Arquivos:**

- Remover: `src/main/kotlin/com/nexus/shopping/notification/`
- Remover: `src/test/kotlin/com/nexus/shopping/notification/`
- Modificar: `src/test/kotlin/com/nexus/shopping/PackageStructureArchitectureTest.kt`
- Modificar: `README.md`
- Modificar: `docs/agents/external-services.md`
- Modificar: `docker-compose.yml`
- Modificar: `AGENTS.md`

**Interfaces:**

- Remove `/notifications` e todas as referencias de producao ao pacote antigo.
- Faz o compose subir Notification Service e seu PostgreSQL em banco e rede separados.

- [ ] **Passo 1: Escrever assertivas de arquitetura e migration antes da remocao**

```kotlin
@Test
fun `codigo de producao nao contem bounded context notification local`() {
    assertFailsWith<ClassNotFoundException> {
        Class.forName("com.nexus.shopping.notification.domain.Notification")
    }
}
```

Estender o contrato de migration para verificar ausencia de `NOTIFICATIONS` e
presenca de `NOTIFICATION_SUBMISSIONS`. Remover assertivas de DTO/excecao antigas
somente depois de adicionar a assertiva de ausencia.

- [ ] **Passo 2: Executar para confirmar a falha**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*PackageStructureArchitectureTest' --tests '*NotificationSubmissionMigrationContractTest'`

Esperado: a classe antiga ainda existe ate a remocao.

- [ ] **Passo 3: Apagar contexto e aplicar topologia exata do Compose**

Apagar as arvores de producao e teste locais, incluindo
`NotificationGatewayAdapter`. Atualizar fakes de Checkout para o novo
`NotificationGateway`. Remover Notification do README, do mapa textual de contextos
e da lista de endpoints.

Adicionar ao `docker-compose.yml` os servicos `notification-service` e
`notification-postgres`. `notification-service` usa `build.context:
../notification-service`, `DATABASE_URL` apontando para
`notification-postgres:5432/notification_db`, `PORT=8080`,
`BASIC_AUTH_USERNAME=notification` e `BASIC_AUTH_PASSWORD=notification`.
`notification-postgres` usa `postgres:16-alpine`, usuario `notification_usr`,
senha `notification_pwd`, banco `notification_db`, volume proprio e healthcheck
`pg_isready -U notification_usr -d notification_db`. Ambos ficam somente na rede
`backend`; `app1`, `app2` e `app3` recebem
`NEXUS_NOTIFICATION_SERVICE_BASE_URL=http://notification-service:8080` e dependem
do inicio do servico, nunca do banco dele diretamente.

Atualizar `docs/agents/external-services.md` para registrar o Notification Service
como dono de runtime e o journal/backoffice como responsabilidade local. Atualizar
`AGENTS.md` apenas onde ainda descreve Notification como contexto local e mantelo
abaixo de 200 linhas.

- [ ] **Passo 4: Procurar referencias obsoletas e executar testes focados**

Executar: `rtk rg -n 'com\.nexus\.shopping\.notification|/notifications|NotificationGatewayAdapter' src/main src/test README.md docs AGENTS.md`

Esperado: nenhuma referencia ao contexto removido; ocorrencias de
`notification-service` e `notification-submissions` sao permitidas.

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*PackageStructureArchitectureTest' --tests '*NotificationSubmissionMigrationContractTest'`

Esperado: PASS.

- [ ] **Passo 5: Commitar remocao e docs**

```bash
rtk git add src/main/kotlin/com/nexus/shopping/notification src/test/kotlin/com/nexus/shopping/notification src/main/kotlin/com/nexus/shopping/integration README.md docs/agents/external-services.md docker-compose.yml AGENTS.md src/test/kotlin/com/nexus/shopping/PackageStructureArchitectureTest.kt
rtk git commit -m "refactor: remove local notification context"
```

### Tarefa 8: Verificar a substituicao completa

**Arquivos:**

- Modificar somente se a verificacao revelar defeito nas Tarefas 1-7.

**Interfaces:**

- Verifica arquitetura, Flyway, contrato HTTP, backoffice, compose e semantica do checkout como uma entrega integrada.

- [ ] **Passo 1: Executar suites de integracao direcionadas**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationSubmission*' --tests '*Checkout*' --tests '*PaymentReconciliationUseCaseTest' --tests '*PaymentServiceProviderGatewayTest'`

Esperado: PASS. Nao enfraquecer nem ocultar falhas preexistentes de cache Redis.

- [ ] **Passo 2: Executar validacao estatica e suite completa**

Executar: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintCheck test`

Esperado: a integracao nova passa. Se continuarem as falhas preexistentes
`ProductRedisCacheIntegrationTest`, registrar seus nomes e a serializacao Jackson
como baseline, sem alterar suas assercoes.

Executar: `rtk git diff --check`

Esperado: nenhum erro de whitespace.

- [ ] **Passo 3: Validar topologia sem subir containers**

Executar: `rtk docker compose config --quiet`

Esperado: codigo de saida 0 e nenhuma variavel de Compose sem resolucao.

- [ ] **Passo 4: Commitar somente correcoes reveladas pela verificacao**

```bash
rtk git add caminho/exato/corrigido
rtk git commit -m "fix: complete notification service integration verification"
```

Nao criar este commit se a verificacao nao exigir correcao.

## Auto-revisao

- Cobertura: Tarefas 1-2 entregam modelo, estados, lease, migration e pagina. Tarefas 3-5 entregam dispatch apos commit nos dois caminhos de aprovacao. Tarefa 4 cobre Basic Auth, idempotencia, timeout e classificacao de erros. Tarefa 6 entrega list/retry/discard. Tarefa 7 remove o contexto e atualiza runtime/docs. Tarefa 8 verifica a entrega.
- Campos pendentes: cada rota, estado, transicao, arquivo, comando e criterio de teste esta definido neste plano.
- Consistencia: Checkout usa `NotificationGateway`; `NotificationSubmissionUseCase` o implementa; o mesmo use case implementa a porta inbound do backoffice; a ACL HTTP e o adapter JPA satisfazem as duas portas outbound.
