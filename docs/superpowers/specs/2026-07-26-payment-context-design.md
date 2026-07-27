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

O contexto e responsavel somente por processar e registrar uma tentativa. Ele nao
altera status de pedidos e nao envia notificacoes.

## Modelo

```text
PaymentAttempt
  id
  attemptReference
  referenceId
  amount
  currency
  status: REQUESTED | APPROVED | REJECTED
  provider
  providerTransactionId
  idempotencyKey
  authorizationFingerprint
  processingLeaseUntil
  processingLeaseToken
  createdAt
  completedAt
```

`PaymentAmount` e `PaymentCurrency` sao tipos proprios de Payment: o valor usa
`BigDecimal` com escala 2, maximo `NUMERIC(12,2)` e e positivo; a moeda e um codigo
ISO 4217 de tres letras sem reutilizar tipos de Order. `provider` e um tipo proprio de
Payment, inicialmente `LOGGING_PROVIDER`.

O token de pagamento nao e persistido nem incluido nos logs. O caso de uso somente
valida que ele esta presente e nao vazio, tratando-o como credencial opaca. A decisao
`approved`/`rejected` e detalhe exclusivo do adapter simulado.

## Gateway do provider

`Payment` define, em `payment/application/port/outbound`, a porta:

```text
PaymentProviderGateway.process(
  referenceId, amount, currency, paymentToken, providerDispatchKey
) -> ProviderProcessingResult
```

Idempotencia tambem e parte do contrato do Gateway: repeticoes da mesma
`providerDispatchKey` devem representar uma unica solicitacao externa e devolver o
mesmo resultado externo. Payment deriva essa chave globalmente unica como
`v1_` seguido de SHA-256 hexadecimal da serializacao canonica com tamanho prefixado de
`provider`, `referenceId` e `idempotencyKey`; portanto a chave tem 67 caracteres, nao
depende de separadores e e segura para limites usuais de providers. Ela nao substitui
a unicidade de dominio `(referenceId, idempotencyKey)`. O Logging Gateway mantem um
`PaymentProviderDispatchJournal` persistente, pertencente ao seu adapter, com
unicidade em `provider_dispatch_key` e persiste essa chave; a primeira insercao
registra o unico envio simulado e os perdedores retornam o resultado ja associado. A
migration cria a tabela tecnica portavel `payment_provider_dispatches`; ela nao e um
modelo de dominio nem uma tabela de outro contexto. Um adapter real encaminha a chave
derivada sem reinterpretacao ao provider.

`LoggingPaymentProviderGateway` fica em `payment/adapter/outbound/provider`. Ele
registra uma unica linha com referencia, valor, moeda e `providerDispatchKey`, sem
token, e retorna deterministicamente:

- `approved` -> `APPROVED` com `providerTransactionId` gerado pelo adapter;
- `rejected` ou qualquer token diferente de `approved` -> `REJECTED`, com
  `providerTransactionId` opcional gerado pelo adapter.

Em uma integracao futura, somente esse adapter sera trocado por um client HTTP de um
provider real para o caminho normal de sucesso/rejeicao. O caso de uso, o modelo e o
repositorio de Payment permanecem independentes do provider; suporte a timeout,
consulta e reentrega exige a extensao declarada mais adiante nesta spec.
O termo `process` significa, nesta etapa didatica, que o provider declarou o pagamento
aprovado ou rejeitado; captura/liquidacao sao casos de uso futuros e distintos.

## Contratos e persistencia

`ProcessPaymentInputPort` e implementado por `ProcessPaymentUseCase`. Recebe
`ProcessPaymentCommand(referenceId, amount, currency, paymentToken, idempotencyKey)`
e devolve `PaymentProcessingResult(attemptReference, referenceId, status,
providerTransactionId?, replayed)`.

`ValidatePaymentInputPort` pertence a Payment e valida valor/moeda antes de qualquer
efeito persistente. O `PaymentValidationGateway` de Integration o chama por seu
adapter local dentro da Transacao A, imediatamente depois de reservar o carrinho e
calcular seu total confiavel, mas antes de criar Order ou confirmar Cart. Assim, um
total que excede `NUMERIC(12,2)` faz rollback de A sem que Payment importe tipos de
Order nem Integration importe tipos de Payment.

Antes de chamar o Gateway, o use case valida referencia, valor, moeda, token e chave
de idempotencia. Ele calcula e persiste `authorizationFingerprint`: HMAC-SHA-256
canonico dos dados de autorizacao. O HMAC usa segredo de configuracao injetado por uma
porta e nunca persiste nem loga o token. O replay compara o fingerprint persistido em
tempo constante; mesma referencia/chave com fingerprint diferente devolve conflito,
inclusive depois de reiniciar a aplicacao.

O repositorio expoe uma reserva atomica: `reserve(...) -> Created | Existing`. O
adapter grava `REQUESTED` com `processingLeaseToken` novo e lease curto, e faz flush em
transacao curta antes de qualquer chamada ao provider. Se perder a unicidade, relê a
tentativa vencedora em uma nova transacao e nunca chama o Gateway. A reclamacao apos
lease expirado troca o token atomicamente. Somente quem possui o token atual chama o
Gateway, fora de transacao de banco, e finaliza APPROVED ou REJECTED por update
condicional em `status=REQUESTED AND processing_lease_token=?`. Uma finalizacao de dono
obsoleto nao sobrescreve a tentativa. Um replay final devolve o registro existente sem
novo log de provider; a idempotencia do Gateway tambem impede um segundo envio externo
quando houver reclamacao apos expiracao.

`REQUESTED` significa que o resultado ainda nao e conhecido. Se uma requisicao encontra
uma tentativa com lease valido, ela aguarda o resultado terminal por um prazo limitado,
sem manter transacao de banco. Se o prazo expira, retorna `REQUESTED` com `replayed` e
o workflow devolve 202 sem atualizar Order. Um replay posterior reconcilia o resultado
terminal. Se o lease expirou sem resultado, uma unica requisicao pode reclamar a
tentativa e reenviar a mesma chave de idempotencia ao provider. Um provider real deve
deduplicar essa chave; o Logging Gateway e testado para uma unica chamada durante um
lease valido. Timeout, consulta e reentrega do provider real continuam fora de escopo,
mas esta politica evita deixar o contrato de REQUESTED indefinido.

Uma migration portavel entre H2 e PostgreSQL cria `payment_attempts`, com unicidade em
`(reference_id, idempotency_key)`, `attempt_reference` unico e nenhum indice redundante.
Nao ha chave estrangeira para `orders`. O workflow gera referencias no namespace
estavel `checkout:<orderId>`; Payment as trata como texto opaco.

## Integracao do checkout

Depois que a spec de fronteiras for aplicada, `integration/checkout/application`
coordena somente seus gateways. Os adapters locais de Integration traduzem cada DTO
para as portas inbound de Order, Payment e Notification; o workflow nao importa
commands, use cases ou tipos de dominio desses contextos:

```text
1. Transacao A: reserva Cart, valida o total/moeda por `ValidatePaymentInputPort`, cria
   Order WAITING_PAYMENT e conclui Cart. A validacao ocorre antes de qualquer escrita.
2. Transacao B: Payment reserva a tentativa; fora de transacao, o adapter loga o envio;
   Transacao C finaliza o resultado.
3. Transacao D: `OrderPaymentResultGateway` aplica idempotentemente o resultado
   persistido.
4. Depois do commit D, `NotificationGateway` assegura a comunicacao de confirmacao.
```

O adapter local de `OrderPaymentResultGateway` chama a operacao inbound de Order, que
recebe apenas dados de seu contrato (`orderId`, attemptReference, status externo e
identificador da transacao). Order nao importa Payment, persiste a referencia opaca da
tentativa sem FK e torna a mesma aplicacao um no-op idempotente. A transicao e
`WAITING_PAYMENT -> CONFIRMED` quando aprovado e `WAITING_PAYMENT -> PAYMENT_FAILED`
quando rejeitado. Um resultado persistido de Payment e sempre reconciliado no replay,
mesmo que tenha sido produzido em uma requisicao anterior. O valor de resposta do
checkout e o Order atualizado.

Notification continua autonomo: `NotificationGateway` recebe `recipientEmail`,
`referenceId`, `ORDER_CONFIRMED`, os parametros `orderId` e `amount` e a chave de evento
`order-confirmed:<orderId>:<attemptReference>`. `referenceId` de Notification continua
sendo o `Long` de Order; ela nao recebe a referencia opaca de Payment. Notification
evolui seu proprio contrato, modelo e migration com `notificationKey` unico e estados
`PENDING -> SENDING -> SENT | FAILED`. Uma operacao idempotente tenta obter um lease
para PENDING, FAILED ou SENDING expirado, envia fora de transacao e atualiza SENT/FAILED
em nova transacao. Cada claim recebe `sendingLeaseToken`; a atualizacao e condicional
ao token e um dono obsoleto nao pode sobrescrever o estado de quem reclamou o envio.
Um replay de SENT nao envia outra vez; um replay de PENDING/FAILED retoma o envio. O
adapter de e-mail existente registra o envio e devolve sucesso. Uma queda apos o log
externo e antes de SENT pode resultar em novo log durante reclaim, a limitacao esperada
de entrega best effort. Pagamento rejeitado nao dispara notificacao nesta entrega.

A notificacao ocorre depois do commit do resultado em Order. Logo, falhar uma futura
integracao de e-mail nao desfaz um pagamento confirmado. Sem outbox, a garantia e
best effort com deduplicacao: uma queda entre o envio externo e sua persistencia pode
exigir reentrega e nao permite prometer exatamente uma entrega. Outbox e entrega
confiavel ficam para a etapa de mensageria.

## Idempotencia e concorrencia

- Repetir checkout com a mesma chave e payload reconcilia Payment e Order, sem nova
  chamada ao provider, e assegura a Notification pela chave de evento.
- A mesma chave com token, valor ou moeda diferente devolve 409; o
  fingerprint do checkout tambem inclui o HMAC do token, sem guardar seu valor bruto.
- A reserva atomica vence a concorrencia antes de chamar o provider: ha uma chamada
  simulada e um resultado persistido para referencia/chave identicas.
- O envio de Notification e deduplicado em condicoes normais pela chave de evento;
  nao e uma garantia de exatamente uma entrega sem outbox.

## Testes de aceitacao

1. Testes puros de Payment confirmam que o caso de uso trata token como opaco e que o
   Logging Gateway decide `approved`/`rejected`; cobrem HMAC, conflito e replay.
2. Teste do `LoggingPaymentProviderGateway` captura logs e confirma que o adapter loga
   o envio sem vazar `paymentToken`.
3. Testes JPA confirmam migration, reserva atomica, unicidade, mapeamento e releitura
   do vencedor em transacao limpa apos conflito; tambem confirmam fingerprint
   persistido e conflito apos reinicializacao do contexto.
4. Teste de arquitetura prova que `payment` nao importa nenhum outro contexto.
5. Testes HTTP do checkout validam `approved` -> 201/200 com `CONFIRMED`, tentativa
   APPROVED e notificacao SENT; e `rejected` -> 201/200 com `PAYMENT_FAILED`, tentativa
   REJECTED e nenhuma notificacao criada.
6. Teste de replay apos Payment aprovado, mas antes de Order confirmado, reconcilia o
   pedido; replay apos Order confirmado assegura Notification sem segunda chamada ao
   provider.
7. Teste com Gateway bloqueado confirma que concorrentes em lease valido aguardam o
   resultado terminal; ao expirar o prazo, recebem REQUESTED/202 sem alterar Order.
8. Teste concorrente com barreira no Gateway confirma N requisicoes identicas, uma
   solicitacao externa/log do provider mesmo apos reclaim, nenhuma excecao de unicidade
   exposta e respostas consistentes. Teste de fencing prova que finalizacao com token
   obsoleto nao altera a tentativa atual.
9. Testes de Notification cobrem reclaim de PENDING/FAILED/SENDING expirado, falha
   antes e depois do envio, fencing de lease e deduplicacao best effort sem outbox.
10. Teste HTTP valida que valor ou moeda fora do contrato de Payment falha antes de
    criar Order ou concluir Cart, apos obter o total confiavel da reserva.
11. Testes do `providerDispatchKey` confirmam serializacao canonica, tamanho fixo,
    persistencia no journal e isolamento de duas referencias com a mesma chave HTTP,
    inclusive quando os valores contem `:`.

## Fora de escopo

- Provider HTTP real, dados de cartao, webhook, estorno e retentativa manual.
- Timeout e consulta de status no provider real; a espera local limitada e o 202 para
  REQUESTED desta entrega permanecem no escopo.
- Endpoint HTTP publico proprio para Payment.
- Broker, outbox e entrega garantida de notificacao.

## Criterio de conclusao

O checkout usa Payment e Notification apenas pela camada `integration/checkout`.
`Payment` pode ser extraido como servico e trocar seu adapter de provider sem depender
de classes, schema ou tabelas de Order.
