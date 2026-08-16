# Fluxo de pagamento por cartao no Pagar.me

Data da pesquisa: 2026-08-10.

Escopo: somente fontes oficiais do Pagar.me, API v5 quando indicada pela
documentacao. Este documento descreve fatos observados e suas implicacoes
diretas para um PSP didatico; nao define a arquitetura do Nexus Shopping.

## Resumo factual

O fluxo de cartao do Pagar.me v5 parte da criacao de um `order`. O pedido
recebe uma lista `payments`; para cartao, cada pagamento informa
`payment_method: credit_card` e o objeto `credit_card`. Um pedido gera ao
menos uma `charge` (cobranca). A transacao de cartao fica em
`charge.last_transaction`.

O tipo de operacao padrao do cartao e `auth_and_capture`: a autorizacao e a
captura acontecem no mesmo fluxo. Portanto, este e o fluxo mais simples de
referencia. O mesmo contrato tambem oferece `auth_only` (autoriza sem cobrar)
e `pre_auth`; a documentacao informa que a pre-autorizacao requer liberacao
pela adquirente.

## Recursos e ciclo de vida observados

### Pedido, cobranca e transacao

- `POST https://api.pagar.me/core/v5/orders` cria o pedido. A referencia
  declara `items` e `payments` obrigatorios e permite um `code` para
  identificar o pedido no sistema da loja.
- O objeto `order` possui estados `Pending`, `Paid`, `Canceled` e `Failed`.
  Por padrao, o pedido e fechado (`closed: true`).
- A documentacao de cobranca afirma que uma cobranca e criada a partir de um
  pedido e que um pedido sempre gera pelo menos uma cobranca.
- No fluxo de cartao, os estados de transacao sao expostos em
  `charge.last_transaction.status`.

Fontes: [criar pedido](https://docs.pagar.me/reference/criar-pedido-2),
[pedidos](https://docs.pagar.me/reference/pedidos-1),
[cobranca](https://docs.pagar.me/docs/cobran%C3%A7a) e
[cartao de credito](https://docs.pagar.me/reference/cart%C3%A3o-de-cr%C3%A9dito-1).

### Autorizacao e captura

- Autorizacao reserva o limite do titular; captura efetiva a cobranca.
- Em `credit_card.operation_type`, o Pagar.me aceita `auth_and_capture`,
  `auth_only` e `pre_auth`. O valor padrao e `auth_and_capture`.
- Quando a captura for posterior, a API v5 expoe
  `POST /core/v5/charges/{charge_id}/capture`. O campo `amount` e opcional;
  se ausente, a API considera o valor integral da cobranca.
- A documentacao do Pagar.me tambem registra que captura total ou parcial e
  cancelamento total ou parcial sao operacoes possiveis para cartao. Para a
  pre-autorizacao, ha dependencia de liberacao pela adquirente.

Fontes: [cartao de credito - guia](https://docs.pagar.me/docs/cart%C3%A3o-de-cr%C3%A9dito),
[cartao de credito - referencia](https://docs.pagar.me/reference/cart%C3%A3o-de-cr%C3%A9dito-1)
e [capturar cobranca](https://docs.pagar.me/reference/capturar-cobran%C3%A7a).

### Estados relevantes de cartao

| Estado da transacao | Significado informado pelo Pagar.me |
| --- | --- |
| `authorized_pending_capture` | Autorizada e pendente de captura. |
| `not_authorized` | Nao autorizada. |
| `captured` | Capturada. |
| `partial_capture` | Capturada parcialmente. |
| `waiting_capture` | Aguardando captura. |
| `refunded` e `partial_refunded` | Estornada total ou parcialmente. |
| `voided` e `partial_void` | Cancelada total ou parcialmente. |
| `waiting_cancellation` | Aguardando cancelamento. |
| `with_error` e `failed` | Com erro ou falha. |

Fonte: [status de cartao de credito](https://docs.pagar.me/reference/cart%C3%A3o-de-cr%C3%A9dito-1).

### Idempotencia

- A chave e fornecida pelo lojista no cabecalho `Idempotency-key`; ela e
  sensivel a maiusculas/minusculas.
- Em producao, a chave expira 24 horas apos o primeiro pedido. No sandbox, a
  janela e de 5 minutos.
- A API armazena a chave e a resposta da criacao; repeticoes na janela recebem
  a mesma transacao, independentemente de o retorno original ser capturado,
  autorizado ou falho.
- Uma mesma chave com corpos distintos ainda produz somente um pedido.
- Uma chamada concorrente enquanto a primeira ainda esta aberta pode receber
  `409 Conflict`. Erros `400` e `500` nao gravam a chave segundo a
  documentacao.

Fonte: [idempotencia](https://docs.pagar.me/docs/o-que-%C3%A9).

### Webhooks e processamento assincrono

- O Pagar.me envia HTTP POST ao endpoint configurado quando ocorre um evento
  e permite configurar mais de um endpoint e selecionar os eventos.
- O objeto de webhook traz, entre outros campos, `event`, `status`,
  `attempts`, `response_status` e `data`. O estado da entrega pode ser
  `pending`, `sent` ou `failed`.
- Entre os eventos pertinentes a cartao estao `order.paid`,
  `order.payment_failed`, `charge.paid`, `charge.payment_failed`,
  `charge.pending`, `charge.processing`, `charge.refunded`,
  `charge.partial_canceled` e `charge.chargedback`. A pagina de eventos
  tambem informa que `charge.chargedback` sera substituido por
  `chargeback.received`, com migracao ate 30/09/2026.

Fontes: [visao geral de webhooks](https://docs.pagar.me/reference/vis%C3%A3o-geral-sobre-webhooks)
e [eventos de webhook](https://docs.pagar.me/reference/eventos-de-webhook-1).

## Sandbox de cartao como referencia

O simulador oficial aceita chaves transacionais de teste e declara que os
fluxos financeiros nao sao utilizados. Para testes de cartao, ele documenta:

- `4000000000000010`: sucesso; com captura, pedido e cobranca ficam pagos e a
  transacao fica capturada.
- `4000000000000028`: falha; pedido e cobranca ficam em falha e a transacao
  fica nao autorizada.
- `4000000000000036`: com captura, inicia com pedido pendente, cobranca em
  processamento e transacao com erro; depois atualiza para capturada e paga.
- `4000000000000044`: inicia de forma equivalente ao caso anterior e depois
  termina em falha.

Fonte: [simulador de cartao de credito](https://docs.pagar.me/docs/simulador-de-cart%C3%A3o-de-cr%C3%A9dito).

## Implicacoes para um PSP didatico v1

As implicacoes abaixo derivam do menor fluxo coberto pelas fontes, sem
pretender reproduzir a superficie completa do Pagar.me:

1. Usar como referencia a criacao de uma cobranca de cartao que conclui
   autorizacao e captura numa unica operacao, pois `auth_and_capture` e o
   padrao declarado pelo Pagar.me.
2. Aceitar uma chave de idempotencia fornecida pelo cliente e preservar a
   associacao entre chave, primeira solicitacao e resultado. Tratar chamada
   concorrente como estado observavel, pois o provedor de referencia responde
   `409` nesse caso.
3. Modelar ao menos resultado aprovado, nao autorizado e processamento que
   termina posteriormente em aprovado ou falha. O simulador oficial mostra
   explicitamente esses quatro comportamentos.
4. Disponibilizar notificacao HTTP para as mudancas assincronas e manter
   metadados de tentativa de entrega, ja que o Pagar.me representa a entrega
   de webhook com status e contador de tentativas.
5. Manter autorizacao sem captura, captura posterior, captura parcial,
   cancelamento/estorno parcial, pre-autorizacao, chargeback, 3DS, tokenizacao,
   parcelamento, antifraude e split como candidatos a evolucao. As fontes os
   apresentam como recursos adicionais ou estados especializados; eles nao
   sao necessarios para reproduzir o fluxo padrao `auth_and_capture`.

## Limites da leitura

Os nomes de recursos e estados acima sao do Pagar.me e nao constituem, por si,
um contrato para outro PSP. A documentacao tambem recomenda nao trafegar dados
abertos de cartao no servidor sem conformidade PCI; para a criacao de pedido,
ela recomenda `card_id` ou `card_token` em vez desses dados.

Fonte: [criar pedido](https://docs.pagar.me/reference/criar-pedido-2).
