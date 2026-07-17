# Spec: Bounded Context Notification (envio de e-mail simulado)

**Status:** Aprovada (subagente especialista, com ajustes aplicados: FK customer_id, EmailSendResult.failureReason, validacao de placeholders obrigatorios)
**Data:** 2026-07-17
**ADR de referencia:** `docs/decisions/2026-07-17-prd-commerce-bounded-contexts.md`

## Contexto

O ADR de bounded contexts define `Notification` como o dominio responsavel por comunicar eventos importantes ao cliente (confirmacao de pedido, falha de pagamento, cancelamento). `Order` e `Payment` ainda nao existem no sistema, entao nesta etapa o contexto `Notification` sera implementado de forma independente e testavel via HTTP, preparado para no futuro ser acionado por eventos de `Order`/`Payment` (sincronos primeiro, depois via broker).

O envio de e-mail sera **simulado**: em vez de chamar um provider real (SendGrid, SES etc.), o adapter apenas loga que o e-mail "foi enviado". Isso espelha a mesma estrategia que o ADR propoe para `Payment` (abstrair o parceiro externo atras de uma porta).

## Escopo

Dentro do escopo:
- Endpoint HTTP para disparar uma notificacao (`POST /notifications`).
- Consulta de notificacao por id (`GET /notifications/{id}`).
- Listagem paginada de notificacoes por cliente (`GET /notifications?customerId=...`).
- Persistencia de notificacoes (auditoria/historico de envio).
- Porta de saida `EmailSenderPort` com adapter que apenas loga (simulacao).
- Tres tipos de notificacao ja previstos no ADR: `ORDER_CONFIRMED`, `ORDER_PAYMENT_FAILED`, `ORDER_CANCELLED`.

Fora do escopo (fica para quando Order/Payment existirem ou quando houver mensageria):
- Consumo de eventos reais (`OrderConfirmed`, `PaymentRejected` etc.) — nesta fase o disparo e via chamada HTTP direta.
- Integracao real com provedor de e-mail.
- Templates de mensagem armazenados em banco (`MessageTemplate` como entidade persistida).
- Multiplos canais (SMS, push) — o modelo permite extensao, mas so `EMAIL` sera implementado.
- Retentativa/fila de notificacoes falhas.

## Modelo de dominio

```kotlin
enum class NotificationType { ORDER_CONFIRMED, ORDER_PAYMENT_FAILED, ORDER_CANCELLED }
enum class NotificationChannel { EMAIL }
enum class NotificationStatus { SENT, FAILED }

data class Notification(
    val id: Long?,
    val customerId: Long,
    val recipientEmail: String,
    val type: NotificationType,
    val channel: NotificationChannel,
    val status: NotificationStatus,
    val subject: String,
    val body: String,
    val referenceId: Long?,
    val createdAt: Instant?,
    val sentAt: Instant?,
)
```

Decisoes de modelagem:
- **Recipient nao e uma entidade separada.** O ADR trata `Customer` como dono dos dados pessoais e evita que outros contextos consultem Customer ao vivo (mesmo principio de snapshot usado em Order). Por isso `Notification` recebe `recipientEmail` diretamente na requisicao (snapshot do e-mail no momento do disparo), em vez de o contexto consultar o Customer.
- **MessageTemplate nao e persistido.** Nesta fase, os templates sao um mapeamento simples `NotificationType -> (subject, body)` resolvido em codigo (`NotificationMessageRenderer`), com placeholders substituidos a partir de um `Map<String, String>` (`templateParams`) enviado na requisicao. Armazenar templates em banco seria over-engineering para o escopo didatico atual.
- **`referenceId`** e um `Long` opcional e solto (sem FK), representando o id do pedido/pagamento relacionado — evita acoplamento com um contexto Order que ainda nao existe.

## Ports

```kotlin
interface NotificationRepositoryPort {
    fun save(notification: Notification): Notification
    fun findById(id: Long): Notification?
    fun findByCustomerId(customerId: Long, page: Int, size: Int): List<Notification> // size+1 para hasNext, sem COUNT
}

interface EmailSenderPort {
    fun send(to: String, subject: String, body: String): EmailSendResult
}

data class EmailSendResult(val success: Boolean, val failureReason: String? = null)
```

`EmailSenderPort` e a fronteira que sera substituida por uma integracao real no futuro (mesma logica de `Payment` abstrair o parceiro externo). O adapter desta fase (`LoggingEmailSenderAdapter`) apenas loga `"Simulated email sent to {to} subject={subject}"` via SLF4J e retorna sucesso.

## Use cases

- `SendNotificationUseCase`: valida comando, resolve subject/body via `NotificationMessageRenderer`, chama `EmailSenderPort`, persiste o resultado (`SENT` se `EmailSendResult.success`, senao `FAILED`) e retorna o `Notification` criado. Validacao (no usecase, nao no adapter): `customerId > 0`, `recipientEmail` nao vazio e com formato basico de e-mail, `type` valido, e todos os placeholders exigidos pelo template resolvido para aquele `type` presentes em `templateParams` (senao lanca `NotificationValidationException` listando a(s) chave(s) ausente(s)).
- `GetNotificationByIdUseCase`: retorna `Notification` ou lanca `NotificationNotFoundException`.
- `ListNotificationsByCustomerUseCase`: retorna slice paginado (`page` default 0, `size` default 50, range `1..500`), seguindo o mesmo contrato de paginacao ja usado em `/products` (`content`, `page`, `size`, `count`, `hasNext`, leitura de `size + 1` linhas, sem `COUNT(*)`).

Exceptions: `NotificationValidationException extends platform.application.exception.ValidationException`, `NotificationNotFoundException extends platform.application.exception.NotFoundException` — mesmo padrao de Customer/Product, tratadas pelo `ApiExceptionHandler` generico ja existente (RFC 7807), sem qualquer mudanca no handler compartilhado.

## Contrato HTTP

```
POST /notifications
Content-Type: application/json

{
  "customerId": 1,
  "recipientEmail": "cliente@example.com",
  "type": "ORDER_CONFIRMED",
  "referenceId": 123,
  "templateParams": { "orderId": "123", "amount": "99.90" }
}

-> 201 Created
{
  "id": 10,
  "customerId": 1,
  "recipientEmail": "cliente@example.com",
  "type": "ORDER_CONFIRMED",
  "channel": "EMAIL",
  "status": "SENT",
  "subject": "Pedido 123 confirmado",
  "body": "Seu pedido 123 no valor de 99.90 foi confirmado.",
  "referenceId": 123,
  "createdAt": "2026-07-17T12:00:00Z",
  "sentAt": "2026-07-17T12:00:00Z"
}
```

```
GET /notifications/{id} -> 200 OK | 404 Not Found (Problem Details)

GET /notifications?customerId=1&page=0&size=50
-> 200 OK
{ "content": [...], "page": 0, "size": 50, "count": 50, "hasNext": true }
```

Validacao de entrada obrigatoria: `customerId`, `recipientEmail`, `type`. `referenceId` e `templateParams` sao opcionais (`templateParams` default `{}`). `GET /notifications` sem `customerId` retorna `400 Bad Request` (Problem Details).

## Persistencia

Migration `V6__create_notification_context.sql`:

```sql
CREATE TABLE notifications (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    recipient_email VARCHAR(254) NOT NULL,
    type VARCHAR(32) NOT NULL,
    channel VARCHAR(16) NOT NULL DEFAULT 'EMAIL',
    status VARCHAR(16) NOT NULL,
    subject VARCHAR(180) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    reference_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    CONSTRAINT notifications_type_check CHECK (type IN ('ORDER_CONFIRMED', 'ORDER_PAYMENT_FAILED', 'ORDER_CANCELLED')),
    CONSTRAINT notifications_channel_check CHECK (channel IN ('EMAIL')),
    CONSTRAINT notifications_status_check CHECK (status IN ('SENT', 'FAILED')),
    CONSTRAINT fk_notifications_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE INDEX idx_notifications_customer_id ON notifications (customer_id);
```

`customer_id` tem FK para `customers(id)`: Notification e Customer ainda vivem no mesmo schema/monolito (o ADR permite banco compartilhado nesta fase), entao a FK e apenas uma constraint de integridade local — nao cria chamada sincrona entre contextos. Sera removida quando Notification ganhar banco proprio. `reference_id` continua sem FK porque `Order` ainda nao existe (nao ha tabela alvo). Portavel entre PostgreSQL e H2, sem tipos exclusivos de Postgres.

## Estrutura de pacotes

```
notification/
  domain/
    Notification.kt
    NotificationType.kt
    NotificationChannel.kt
    NotificationStatus.kt
    NotificationMessageRenderer.kt   # regra pura: type + params -> subject/body
  application/
    command/SendNotificationCommand.kt
    port/outbound/NotificationRepositoryPort.kt
    port/outbound/EmailSenderPort.kt
    usecase/SendNotificationUseCase.kt
    usecase/GetNotificationByIdUseCase.kt
    usecase/ListNotificationsByCustomerUseCase.kt
    exception/NotificationNotFoundException.kt
    exception/NotificationValidationException.kt
  adapter/
    inbound/http/NotificationController.kt
    inbound/http/dto/SendNotificationRequest.kt
    inbound/http/dto/NotificationResponse.kt
    outbound/jpa/NotificationEntity.kt
    outbound/jpa/SpringDataNotificationRepository.kt
    outbound/jpa/NotificationJpaRepositoryAdapter.kt
    outbound/email/LoggingEmailSenderAdapter.kt
```

`domain/` e `application/` sem imports de `jakarta.persistence`, `org.hibernate` ou `org.springframework.data` — mesma regra fixada no `CLAUDE.md`.

## Testes

Espelhando o padrao de Customer:
- `SendNotificationUseCaseTest`, `GetNotificationByIdUseCaseTest`, `ListNotificationsByCustomerUseCaseTest` — com fakes em memoria dos ports (`NotificationRepositoryPort`, `EmailSenderPort`), incluindo caso de falha de validacao por `templateParams` com chave obrigatoria ausente.
- `NotificationControllerTest` — `@SpringBootTest(webEnvironment=RANDOM_PORT)`, H2 + Flyway real, `HttpClient` batendo nos 3 endpoints.
- `adapter/outbound/jpa/NotificationEntityTest` — `toDomain()`/`toEntity()`.
- Teste de contrato de migration (`NotificationMigrationContractTest`), mesmo padrao de `CustomerMigrationContractTest`, incluindo a FK `fk_notifications_customer`.
- `PackageStructureArchitectureTest` (raiz do projeto) ja valida automaticamente que o novo contexto segue as regras (exceptions estendendo bases da platform, `ApiExceptionHandler` sem import de contexto, DTOs fora do domain) — nenhuma mudanca nesse teste e necessaria.

## Fora de escopo / riscos aceitos

- Sem retry de e-mail falho (o ADR nao pede isso agora).
- Sem idempotencia de disparo (ainda nao ha orquestrador de checkout chamando isso).
- `templateParams` ausente para uma chave exigida pelo template lanca `NotificationValidationException` (400).
- Ordem `send()` antes de `save()`: em caso de falha do `save()` apos um `send()` bem-sucedido, o envio nao tem registro de auditoria persistido; aceito nesta fase porque `LoggingEmailSenderAdapter` nao falha e a unica falha concreta de `save()` (FK de `customerId`) ja e tratada antes do envio ser considerado bem-sucedido pelo cliente da API — revisitar (com porta de update + status `PENDING`) quando um `EmailSenderPort` real for introduzido.
