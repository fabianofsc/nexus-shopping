# Spec: nexus-payment-service como unico provider de pagamento

**Status:** Proposta
**Data:** 2026-08-14
**Pre-requisito:** `2026-08-13-nexus-payment-service-adapter-design.md` (adapter opcional, ainda em revisao na PR #27)

## Objetivo

A PR #27 introduziu `NexusPaymentServiceProviderGateway` coexistindo com o
`LoggingPaymentProviderGateway` simulado, atras de um toggle de configuracao
(`nexus.payment-service.enabled`). Esta spec fecha esse capitulo: o gateway
simulado e deletado, o `nexus-payment-service` passa a ser a **unica** forma
de processar pagamentos, e vira uma dependencia obrigatoria de runtime do
nexus-shopping — nao mais uma integracao opcional demonstrada manualmente.

Motivacao: o toggle e o gateway simulado existiam para permitir uma transicao
gradual e testes 100% offline. Esse valor didatico se esgota agora — o
proximo passo da trilha e o aluno **sempre** operar contra o Payment Service
real, inclusive nos testes automatizados, para experimentar comunicacao HTTP
entre servicos como parte central do aprendizado, nao como excecao opcional.

## Fora de escopo

- Publicar a imagem `fabianofsc/nexus-payment-service:latest` no Docker Hub.
  Este trabalho e paralelo, feito no repositorio `nexus-payment-service` por
  fora desta spec. O design aqui assume que a imagem existe (mesmo padrao ja
  usado por `fabianofsc/dummy-pay:latest`).
- Qualquer mudanca no repositorio `nexus-payment-service` em si.
- Automatizar o registro do webhook subscription do Dummy Pay (passo manual
  hoje, fora de banda) — segue sendo responsabilidade de quem sobe a stack.

## Nomenclatura

Sem mais dois gateways coexistindo, os prefixos que descreviam a *diferenca*
entre eles (`Logging` = simulado, `Nexus` = o nome do projeto que fala com
ele) deixam de fazer sentido — a classe deve ser nomeada pelo que faz, como
se fosse a unica implementacao possivel (porque agora e):

| Antes | Depois |
| --- | --- |
| `NexusPaymentServiceProviderGateway` | `PaymentServiceProviderGateway` |
| `PaymentProvider.NEXUS_PAYMENT_SERVICE` | `PaymentProvider.PAYMENT_SERVICE` (unico valor do enum) |
| `PaymentProvider.LOGGING_PROVIDER` | removido |

O arquivo/teste `NexusPaymentServiceProviderGatewayTest.kt` e renomeado para
`PaymentServiceProviderGatewayTest.kt`, mantendo `MockRestServiceServer` — e
um teste unitario de mapeamento HTTP isolado, nao precisa de WireMock.

## Deletado

- `payment/adapter/outbound/provider/LoggingPaymentProviderGateway.kt`
- `payment/adapter/outbound/provider/LoggingPaymentProviderGatewayTest.kt`
- `payment/adapter/outbound/provider/PaymentProviderDispatchEntity.kt`
- `payment/adapter/outbound/provider/SpringDataPaymentProviderDispatchRepository.kt`
- `checkout/PaymentCheckoutReconciliationHttpTest.kt` — testava
  replay idempotente atraves de janelas de falha assumindo que Payment
  resolvia de forma sincrona (Payment -> Order -> Notification na mesma
  transacao HTTP). Essa premissa nao existe mais: o dispatch real sempre
  retorna `PROCESSING`; nao ha "janela de falha entre Payment e Order" porque
  os dois nunca acontecem na mesma requisicao.

A tabela `payment_provider_dispatches` fica orfa (so era escrita pelo
Logging). Nova migration `V13__drop_payment_provider_dispatches.sql`:

```sql
DROP TABLE payment_provider_dispatches;
```

Nunca editar `V10__create_payment_provider_dispatch_journal.sql` — a
convencao do projeto e migrations imutaveis uma vez aplicadas; a remocao e
uma migration nova.

## Configuracao

Remove o toggle inteiro. `PaymentServiceProviderGateway` e
`PaymentReconciliationScheduler` deixam de ter `@ConditionalOnProperty` —
viram `@Component`/`@Component` incondicionais, unicos beans de
`PaymentProviderGateway` no contexto.

```yaml
nexus:
  payment-service:
    base-url: ${NEXUS_PAYMENT_SERVICE_BASE_URL:http://localhost:8081}
    polling-interval: ${NEXUS_PAYMENT_SERVICE_POLLING_INTERVAL:2000}
```

`RestClientConfig.kt` (bean `RestClient.Builder`, criado durante a
verificacao manual da PR #27 porque o Spring Boot 4.1.0 nao autoconfigura
esse bean neste stack) continua como esta.

## Impacto no fluxo de checkout

Com o gateway real como unica opcao, **todo** checkout passa a ser
assincrono: `POST /v1/payments` do nexus-payment-service sempre responde
`202 PROCESSING`, nunca um resultado terminal inline. Isso muda o
comportamento observavel de `POST /customers/{id}/cart/checkout` **sempre**
— nao e mais um caso especial testado a parte:

- A resposta imediata do checkout e sempre `202 Accepted` com o pedido em
  `WAITING_PAYMENT` (o branch que ja existia em
  `ExecuteCheckoutUseCase.execute()` para `PaymentResultStatus.REQUESTED`
  deixa de ser um caso raro e passa a ser o unico caminho).
- A confirmacao (`CONFIRMED`/`PAYMENT_FAILED`) so chega depois, via
  `PaymentReconciliationScheduler` rodando em background (ou, em teste,
  chamando `PaymentReconciliationUseCase.reconcile()` diretamente — ver
  secao de testes).

## Estrategia de testes

Dependencia nova: `org.wiremock.integrations:wiremock-spring-boot`
(`testImplementation`), versao 4.x (confirmada compativel com Spring Boot 4;
`4.0.9` e a versao minima verificada, mas a implementacao deve checar se ha
uma 4.x mais recente no momento). Compatibilidade exata com Spring Framework
7 precisa ser validada na implementacao — se houver problema, o ponto de
impacto fica isolado nos testes `@SpringBootTest` de checkout, sem tocar
producao.

Uso: `@EnableWireMock({@ConfigureWireMock(name = "nexus-payment-service", baseUrlProperties = "nexus.payment-service.base-url")})`
na classe de teste. O WireMock sobe em porta aleatoria e a propriedade
`nexus.payment-service.base-url` e sobrescrita automaticamente — o
`PaymentServiceProviderGateway` de producao passa a falar com o WireMock sem
saber disso.

Padrao de stub por teste: `POST /v1/payments` sempre responde
`202 {attemptReference: "<gerado>", status: "PROCESSING"}`; `GET
/v1/payments/{attemptReference}` responde o status terminal desejado pelo
cenario (`APPROVED`/`REJECTED`). Como o scheduler nao roda durante os testes
(ver decisao abaixo), os testes chamam `PaymentReconciliationUseCase`
diretamente apos configurar o stub de `GET`, tornando a espera
deterministica — sem sleep, sem Awaitility, sem depender do
`@Scheduled` disparar dentro da janela do teste.

### Arquivos de teste afetados

- **`PaymentCheckoutHttpTest.kt`** — reescrito. Cada teste que hoje espera
  `CONFIRMED`/`PAYMENT_FAILED` na resposta imediata do checkout passa a:
  checkout (asserta `202`/`WAITING_PAYMENT`) -> stub WireMock do `GET` com o
  resultado terminal -> chama `PaymentReconciliationUseCase.reconcile()` ->
  consulta o pedido de novo e asserta o status terminal. Assercoes em
  `payment_provider_dispatches` viram assercoes equivalentes em
  `payment_attempts`.
- **`PaymentCheckoutConcurrencyHttpTest.kt`** — reescrito. O que hoje testa
  ("checkouts concorrentes com a mesma idempotency key geram um so dispatch e
  uma resposta consistente") continua valido, mas a resposta consistente
  agora e `WAITING_PAYMENT` para todas as chamadas concorrentes (nao mais
  `CONFIRMED`), e a asserção de "um so dispatch" passa a contar chamadas
  `POST /v1/payments` recebidas pelo WireMock (`verify(1, postRequestedFor(...))`)
  em vez de linhas em `payment_provider_dispatches`.
- **`CheckoutWorkflowIntegrationTest.kt`** — os dois testes existentes (rollback
  de transacao) usam fakes in-memory dos gateways de outbound do checkout, nao
  o gateway de payment real — nao dependem de HTTP nenhum. Sem mudanca
  estrutural; so precisam compilar contra a interface renomeada.
- **`PaymentCheckoutReconciliationHttpTest.kt`** — removido (ver secao
  "Deletado").
- **`PaymentRequestedCheckoutHttpTest.kt`** — simplificado. O wrapper
  `BlockingPaymentProvider` existia para forcar artificialmente o caminho
  `REQUESTED` sobre um gateway que normalmente resolvia sincrono. Isso deixa
  de ser necessario: **todo** checkout agora entra em `REQUESTED` de forma
  natural, sem truque nenhum. O teste vira: checkout -> `202`/`WAITING_PAYMENT`
  -> replay com a mesma idempotency key ainda `202`/`WAITING_PAYMENT` (o
  loop de espera limitado em `ProcessPaymentUseCase.replay()` estoura o
  timeout de 500ms porque nada resolve o attempt sincronamente) -> stub do
  `GET` + `reconcile()` -> novo replay confirma `CONFIRMED`. `BlockingPaymentProvider`
  e `PaymentProviderGateway` fake sao deletados deste arquivo.
- **`PaymentServiceProviderGatewayTest.kt`** (ex-`NexusPaymentServiceProviderGatewayTest.kt`)
  — sem mudanca de estrategia, so o rename e ajuste de import/nome de classe.
- **`ReconcilePendingPaymentAttemptsUseCaseTest.kt`**, **`ProcessPaymentUseCaseTest.kt`**,
  **`OrderCheckoutBoundaryTest.kt`** — usam fakes proprios de
  `PaymentProviderGateway`/`PaymentAttemptRepositoryPort`, nao a implementacao
  real. So precisam do rename `PaymentProvider.NEXUS_PAYMENT_SERVICE` ->
  `PaymentProvider.PAYMENT_SERVICE`.

## Docker Compose

`docker-compose.yml` do nexus-shopping ganha a topologia inteira do
Payment Service, referenciando imagens publicadas (sem build local, sem
clonar o repo irmao — o aluno so precisa do repo nexus-shopping):

```yaml
services:
  nexus-payment-service:
    image: fabianofsc/nexus-payment-service:latest
    environment:
      DB_URL: jdbc:postgresql://nexus-payment-postgres:5432/nexus_payment
      DB_USERNAME: nexus
      DB_PASSWORD: nexus
      NEXUS_PAYMENT_DUMMYPAY_BASE_URL: http://dummypay:8080
      SERVER_PORT: "8081"
      NEXUS_PAYMENT_AUTHORIZATION_FINGERPRINT_SECRET: ${NEXUS_PAYMENT_AUTHORIZATION_FINGERPRINT_SECRET:-dev-secret-change-in-production}
      NEXUS_PAYMENT_DUMMYPAY_KEY_ID: ${NEXUS_PAYMENT_DUMMYPAY_KEY_ID:-local-dev-account}
      NEXUS_PAYMENT_DUMMYPAY_KEY_SECRET: ${NEXUS_PAYMENT_DUMMYPAY_KEY_SECRET:-change-me-to-a-long-random-value}
      NEXUS_PAYMENT_DUMMYPAY_WEBHOOK_SECRET: ${NEXUS_PAYMENT_DUMMYPAY_WEBHOOK_SECRET:-dev-webhook-secret}
    depends_on:
      nexus-payment-postgres:
        condition: service_healthy
      dummypay:
        condition: service_started
    networks:
      - backend

  nexus-payment-postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: nexus_payment
      POSTGRES_USER: nexus
      POSTGRES_PASSWORD: nexus
    volumes:
      - nexus-payment-pgdata:/var/lib/postgresql/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U nexus -d nexus_payment"]
      interval: 2s
      timeout: 2s
      retries: 15

  dummypay-postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: dummypay
      POSTGRES_USER: dummypay
      POSTGRES_PASSWORD: dummypay
    volumes:
      - dummypay-pgdata:/var/lib/postgresql/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dummypay -d dummypay"]
      interval: 2s
      timeout: 2s
      retries: 15

  dummypay:
    image: fabianofsc/dummy-pay:latest
    environment:
      DUMMYPAY_HTTP_ADDR: ":8080"
      DUMMYPAY_DATABASE_URL: "postgres://dummypay:dummypay@dummypay-postgres:5432/dummypay?sslmode=disable"
      DUMMYPAY_ACCOUNT_KEY_ID: ${NEXUS_PAYMENT_DUMMYPAY_KEY_ID:-local-dev-account}
      DUMMYPAY_ACCOUNT_KEY_SECRET: ${NEXUS_PAYMENT_DUMMYPAY_KEY_SECRET:-change-me-to-a-long-random-value}
      DUMMYPAY_WEBHOOK_SECRET_ENC_KEY: "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
      DUMMYPAY_PROCESSING_DELAY: "3s"
      DUMMYPAY_IDEMPOTENCY_LEASE: "30s"
      DUMMYPAY_WORKER_POLL_INTERVAL: "250ms"
      DUMMYPAY_WEBHOOK_TIMEOUT: "5s"
    depends_on:
      dummypay-postgres:
        condition: service_healthy
    networks:
      - backend
```

Nenhuma porta extra exposta ao host: `dummypay` usaria a mesma porta host
`8080` que o nginx do nexus-shopping ja ocupa, entao fica so na rede interna
`backend`, alcancavel pelos apps do nexus-shopping via
`http://nexus-payment-service:8081`. `NEXUS_PAYMENT_SERVICE_BASE_URL` no
bloco `x-app` do compose passa a ser `http://nexus-payment-service:8081`.

## Documentacao

- `CLAUDE.md`: linha de Stack ganha mencao ao WireMock; linha de dependencias
  externas passa a citar `nexus-payment-service` como dependencia obrigatoria
  de runtime (nao mais so `docker compose up -d postgres`).
- `README.md`: secao "Servicos externos autonomos" e o bullet "`Payment`
  continua sendo a primeira fronteira de extracao..." ficam desatualizados
  (descrevem um estado "ainda nao integrado" que deixa de ser verdade) — API
  reescrita para refletir que o Payment Service ja e consumido. Secao
  "Executar localmente" ganha nota de que `docker compose up` agora sobe o
  Payment Service tambem.
- `docs/agents/external-services.md`: secao "Estado" e "Sequencia de
  evolucao" atualizadas — o passo 3 ("Refatorar o Nexus para substituir o
  provider de Payment local pelo adapter HTTP do Payment Service") passa de
  planejado para feito.

## Criterio de conclusao

- Nenhuma referencia a `LoggingPaymentProviderGateway`,
  `PaymentProviderDispatchEntity` ou `PaymentProvider.LOGGING_PROVIDER` resta
  no codigo.
- `./gradlew build` verde usando exclusivamente `PaymentServiceProviderGateway`
  contra WireMock — nenhum teste depende de rede real ou do
  nexus-payment-service de fato rodando.
- `docker compose up` a partir de um clone limpo do nexus-shopping (sem o
  repo `nexus-payment-service` presente localmente) sobe uma stack funcional
  ponta a ponta, assumindo que `fabianofsc/nexus-payment-service:latest`
  existe no Docker Hub.
- Checkout com token de aprovacao responde `202`/`WAITING_PAYMENT`; apos o
  scheduler rodar (ou reconciliacao manual em teste), o pedido vira
  `CONFIRMED` com notificacao unica. Recusa vira `PAYMENT_FAILED` sem
  notificacao — mesmo comportamento observavel ja validado manualmente na
  PR #27, agora como unico caminho possivel, nao mais opcional.
