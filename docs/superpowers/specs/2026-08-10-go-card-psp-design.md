# Spec: PSP de cartao em Go para o ecossistema Payment

**Status:** Aprovada
**Data:** 2026-08-10

## Objetivo

Criar primeiro um PSP autonomo, implementado em Go, para representar um
provedor externo de pagamentos por cartao. Ele sera integrado posteriormente
por um Payment Service independente e somente depois o Nexus Shopping deixara
de usar o provider local atual.

O PSP nao e parte do dominio do Nexus e nao importa, consulta ou compartilha
banco com Nexus, Order ou Payment Service. Ele sera desenvolvido em sessao e
repositorio proprios.

## Referencia e limite da primeira versao

O comportamento de referencia e o fluxo de cartao do Pagar.me v5. A operacao
padrao do Pagar.me e `auth_and_capture`: autorizacao e captura acontecem na
mesma solicitacao. A V1 do PSP adota essa venda unica, sem reproduzir os
recursos de `order` e `charge` do Pagar.me.

O PSP usa contrato e nomes proprios. Pagar.me e apenas referencia de
comportamento para idempotencia, processamento assincrono e webhooks.

## Topologia e ownership

```text
Payment Service --HTTP/Basic Auth--> PSP de cartao em Go --webhook HMAC--> Payment Service
                                     PostgreSQL proprio
```

O PSP possui seu proprio PostgreSQL. Sao seus dados: credenciais de conta
tecnica, transacoes, chaves de idempotencia, assinaturas de webhook e entregas
de webhook. Nenhum outro servico le ou escreve essas tabelas.

Na V1 ha uma conta tecnica por ambiente. O cliente autentica chamadas com
Basic Auth e uma chave secreta configurada por ambiente.

## Contrato V1 do PSP

### Criar pagamento de cartao

`POST /v1/payments`

Headers obrigatorios:

- `Authorization: Basic <credencial da conta tecnica>`
- `Idempotency-Key: <chave nao vazia>`

Corpo:

```json
{
  "reference_id": "checkout:123",
  "amount": 10990,
  "currency": "BRL",
  "payment_token": "card_processing_approved"
}
```

`amount` e inteiro positivo em centavos. `reference_id` e opaco para o PSP.
`payment_token` e opaco e representa somente um cenario controlado; a V1 nao
aceita, persiste ou registra PAN, CVV, validade ou dados reais de cartao.

Uma mesma `Idempotency-Key` para a mesma conta devolve a transacao criada na
primeira solicitacao. A mesma chave com outro corpo nao pode criar nova
transacao. Uma solicitacao concorrente ainda em processamento recebe `409`.

Resposta terminal aprovada:

```json
{
  "payment_id": "pay_01...",
  "reference_id": "checkout:123",
  "status": "APPROVED",
  "provider_transaction_id": "txn_01..."
}
```

Estados expostos pela V1: `APPROVED`, `REJECTED` e `PROCESSING`.

Tokens obrigatorios:

- `card_approved`: resposta e estado `APPROVED`.
- `card_declined`: resposta e estado `REJECTED`.
- `card_processing_approved`: resposta `PROCESSING`, seguida de `APPROVED`.
- `card_processing_declined`: resposta `PROCESSING`, seguida de `REJECTED`.

O atraso dos dois cenarios `PROCESSING` deve ser configuravel por ambiente e
deterministico em testes. Cada transacao terminal tem um
`provider_transaction_id` estavel.

### Assinaturas administrativas de webhook

`POST /v1/webhook-subscriptions` cria uma assinatura previamente, e nao por
pagamento. A requisicao e autenticada pela mesma conta tecnica.

```json
{
  "url": "http://payment-service:8080/internal/provider-events",
  "events": ["payment.approved", "payment.rejected", "payment.processing"]
}
```

A resposta retorna o identificador da assinatura e um segredo exibido uma
unica vez. O PSP guarda o segredo cifrado com uma chave de ambiente separada
da chave da conta tecnica; apenas a rotina de entrega o le para assinar. A
assinatura pode ser ativada ou desativada, mas a V1 precisa de apenas uma
assinatura ativa por conta tecnica.

Para cada evento assinado, o PSP envia HTTP POST com JSON proprio e header
`X-Webhook-Signature`, calculado por HMAC-SHA-256 sobre o corpo bruto com o
segredo da assinatura. O consumidor valida a assinatura antes de processar o
evento.

```json
{
  "event_id": "evt_01...",
  "type": "payment.approved",
  "created_at": "2026-08-10T12:00:00Z",
  "data": {
    "payment_id": "pay_01...",
    "reference_id": "checkout:123",
    "status": "APPROVED",
    "provider_transaction_id": "txn_01..."
  }
}
```

O PSP persiste a entrega antes da chamada HTTP. Cada entrega possui estado
`PENDING`, `SENT` ou `FAILED`, contador de tentativas, ultimo horario e ultimo
status HTTP. Falha de rede ou resposta nao-2xx permite retentativa. A V1 deve
oferecer `POST /v1/webhook-deliveries/{delivery_id}/retry` para reenvio
administrativo.

## Fora de escopo da V1

- Tokenizacao, armazenamento de cartao, PAN, CVV e requisitos PCI reais.
- `auth_only`, pre-autorizacao, captura posterior ou parcial.
- Cancelamento, estorno, chargeback e disputa.
- Parcelamento, 3DS, antifraude, split, recorrencia, Pix e boleto.
- Multi-conta, dashboard, API publica, rate limiting e rotacao automatica de
  chaves.
- Failover automatico entre PSPs. Um timeout ambiguo deve ser reconciliado,
  nunca cobrado novamente em outro provedor sem confirmacao de estado.

## Sequencia obrigatoria de entrega

1. Implementar, testar e operar localmente o PSP em Go como servico autonomo.
   Ele nao recebe dependencia, pacote ou detalhe do Nexus Shopping.
2. Implementar o Payment Service independente, com seu modelo de tentativas e
   um adapter HTTP para o PSP. Ele deve reconciliar respostas e webhooks sem
   delegar sua idempotencia de negocio ao PSP.
3. Refatorar o Nexus Shopping: substituir o adapter local de Payment pela
   chamada HTTP ao Payment Service e preservar os contratos de checkout, Order
   e Notification.

Cada etapa exige spec, plano, testes e revisao proprios. Nao iniciar a etapa
seguinte sem a anterior entregue e aceita.

## Criterios de aceitacao da V1 do PSP

1. O servico sobe sem depender de Nexus ou Payment Service e usa PostgreSQL
   proprio.
2. Basic Auth protege as APIs de pagamento e administracao.
3. Os quatro tokens de cenario produzem os estados definidos e nunca vazam
   dados sensiveis em logs.
4. Idempotencia e concorrencia preservam uma unica transacao por chave.
5. `PROCESSING` produz evento terminal por webhook; webhook e entregue com
   HMAC, e tentativas sao persistidas e podem ser reenviadas.
6. A suite automatizada nao precisa de rede externa nem de credenciais de
   Pagar.me, Mercado Pago ou Stripe.

## Fontes de referencia

- [Pagar.me: cartao de credito](https://docs.pagar.me/reference/cart%C3%A3o-de-cr%C3%A9dito-1)
- [Pagar.me: idempotencia](https://docs.pagar.me/docs/o-que-%C3%A9)
- [Pagar.me: simulador de cartao](https://docs.pagar.me/docs/simulador-de-cart%C3%A3o-de-cr%C3%A9dito)
- [Pagar.me: webhooks](https://docs.pagar.me/reference/vis%C3%A3o-geral-sobre-webhooks)
- [Pagar.me: eventos de webhook](https://docs.pagar.me/reference/eventos-de-webhook-1)
