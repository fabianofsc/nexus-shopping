# Spec: Bounded Context Payment com Gateway de provider simulado

**Status:** Aprovada, condicionada a implementacao da spec de fronteiras do Checkout
**Data:** 2026-07-26
**Pre-requisito:** `2026-07-26-checkout-integration-boundaries-design.md`

## Objetivo

Implementar `Payment` como Bounded Context autonomo, com tentativas persistidas,
idempotencia e um `PaymentProviderGateway` cujo adapter apenas registra que enviou a
requisicao ao provider. O token recebido no checkout decide o resultado de forma
deterministica. A aprovacao atualiza o pedido por meio do workflow de integracao e
dispara uma notificacao cujo adapter tambem apenas registra o envio.

## Limite do contexto

`Payment` nao pode importar pacotes `order`, `cart`, `customer`, `notification` ou
`integration`; nao possui FK para tabelas de outros contextos. A origem da cobranca e
representada pelo valor opaco `referenceId: String`, recebido pelo seu contrato. O
workflow mapeia o id do pedido para essa referencia sem expor o modelo de Order a
Payment.

O contexto e responsavel somente por autorizar e registrar uma tentativa. Ele nao
altera status de pedidos e nao envia notificacoes.

## Modelo

```text
PaymentAttempt
  id
  referenceId
  amount
  currency
  status: REQUESTED | APPROVED | REJECTED
  provider
  providerTransactionId
  idempotencyKey
  createdAt
  completedAt
```

O token de pagamento nao e persistido nem incluido nos logs. Ele e apenas uma entrada
de simulacao. Os valores permitidos sao exatamente `approved` e `rejected`; qualquer
outro valor e erro de validacao 400.

## Gateway do provider

`Payment` define, em `payment/application/port/outbound`, a porta:

```text
PaymentProviderGateway.authorize(
  referenceId, amount, currency, paymentToken, idempotencyKey
) -> ProviderAuthorizationResult
```

`LoggingPaymentProviderGateway` fica em `payment/adapter/outbound/provider`. Ele
registra uma unica linha com referencia, valor, moeda e chave de idempotencia, sem
token, e retorna deterministicamente:

- `approved` -> `APPROVED` com `providerTransactionId` gerado pelo adapter;
- `rejected` -> `REJECTED` com `providerTransactionId` gerado pelo adapter.

Em uma integracao futura, somente esse adapter sera trocado por um client HTTP de um
provider real. O caso de uso, o modelo e o repositorio de Payment permanecem iguais.

## Contratos e persistencia

`AuthorizePaymentInputPort` e implementado por `AuthorizePaymentUseCase`. Recebe
`AuthorizePaymentCommand(referenceId, amount, currency, paymentToken, idempotencyKey)`
e devolve `PaymentAuthorizationResult(attemptId, referenceId, status,
providerTransactionId, newlyAuthorized)`.

Antes de chamar o Gateway, o use case valida referencia, valor positivo, moeda ISO de
tres letras, token e chave de idempotencia. Ele busca uma tentativa com a combinacao
`referenceId + idempotencyKey`; em replay, devolve o resultado persistido sem chamar
o provider nem registrar novo log. Na primeira chamada, persiste REQUESTED, chama o
Gateway e persiste APPROVED ou REJECTED.

Uma migration portavel entre H2 e PostgreSQL cria `payment_attempts`, com unicidade em
`(reference_id, idempotency_key)` e indice para essa consulta. Nao ha chave estrangeira
para `orders`.

## Integracao do checkout

Depois que a spec de fronteiras for aplicada, `integration/checkout` passa a coordenar
as portas de entrada/gateways de Order, Payment e Notification:

```text
1. Cria Order WAITING_PAYMENT na transacao local de Cart + Order.
2. Chama Payment com referenceId opaco, total, moeda, token e chave de idempotencia.
3. Chama a porta de entrada de Order para aplicar o resultado por um contrato proprio.
4. Se o resultado e APPROVED e a tentativa e nova, chama Notification.
```

Order expoe uma operacao de aplicacao de resultado que recebe apenas dados de seu
contrato (`orderId`, status externo e identificador da transacao). Ela nao importa
Payment. A transicao e `WAITING_PAYMENT -> CONFIRMED` quando aprovado e
`WAITING_PAYMENT -> PAYMENT_FAILED` quando rejeitado. O valor de resposta do checkout
e o Order atualizado.

Notification continua autonomo: o workflow envia `recipientEmail`, `referenceId`,
`ORDER_CONFIRMED` e os parametros `orderId` e `amount`. O adapter de e-mail existente
registra o envio e devolve sucesso. Pagamento rejeitado nao dispara notificacao nesta
entrega.

A notificacao ocorre depois da persistencia do resultado em Order e fora da transacao
de Cart + Order. Logo, falhar uma futura integracao de e-mail nao desfaz um pagamento
confirmado. Outbox e reentrega confiavel ficam para a etapa de mensageria.

## Idempotencia e concorrencia

- Repetir checkout com a mesma chave e payload nao cria tentativa, nao chama provider
  novamente e nao registra outra notificacao de confirmacao.
- Chaves iguais com payload de checkout diferente preservam o conflito 409 existente.
- A unicidade do banco e a consulta por tentativa protegem chamadas concorrentes de
  Payment para a mesma referencia e chave.
- O workflow trata uma tentativa persistida como replay, inclusive se chegar por uma
  nova requisicao HTTP.

## Testes de aceitacao

1. Testes puros de Payment validam ambos os tokens, validacao e replay idempotente.
2. Teste do `LoggingPaymentProviderGateway` captura logs e confirma que o adapter loga
   o envio sem vazar `paymentToken`.
3. Testes JPA confirmam migration, unicidade, mapeamento e consulta sem `COUNT(*)
   desnecessario.
4. Teste de arquitetura prova que `payment` nao importa nenhum outro contexto.
5. Testes HTTP do checkout validam `approved` -> 201/200 com `CONFIRMED`, tentativa
   APPROVED e notificacao SENT; e `rejected` -> 201/200 com `PAYMENT_FAILED`, tentativa
   REJECTED e nenhuma notificacao criada.
6. Teste de replay confirma uma unica tentativa e uma unica notificacao, inclusive sob
   concorrencia.

## Fora de escopo

- Provider HTTP real, dados de cartao, webhook, estorno e retentativa manual.
- Timeout e estado PAYMENT_PROCESSING.
- Endpoint HTTP publico proprio para Payment.
- Broker, outbox e entrega garantida de notificacao.

## Criterio de conclusao

O checkout usa Payment e Notification apenas pela camada `integration/checkout`.
`Payment` pode ser extraido como servico e trocar seu adapter de provider sem depender
de classes, schema ou tabelas de Order.
