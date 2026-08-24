# Servicos externos autonomos

## Estado

DummyPay e Notification Service estao implementados como servicos Go em
repositorios independentes. Na arvore local atual, ficam respectivamente em
`../dummy-pay` e `../notification-service`.

DummyPay ainda nao esta integrado diretamente ao runtime deste repositorio; e falado
exclusivamente pelo Payment Service. O Nexus **ja** consome o Payment Service
(`nexus-payment-service`) real via HTTP/ACL para processar pagamentos — o
adapter simulado local foi removido. O Nexus tambem consome o Notification
Service por HTTP/ACL no Checkout; o contexto local Notification foi removido.
Nenhuma dependencia de codigo, submodulo, tabela ou acesso cruzado a banco foi
introduzida em nenhum dos dois casos.

## Topologia alvo

```text
Nexus Shopping --HTTP/ACL--> Payment Service --HTTP--> DummyPay
      |
      +--HTTP/ACL--> Notification Service
```

O Payment Service ainda sera criado. Ele e dono das regras de pagamento do
ecossistema e escolhe o adapter de PSP. DummyPay e apenas um PSP de cartao
deterministico, equivalente a uma integracao externa simulada.

O Notification Service e generico: recebe destino, mensagem renderizada e
referencias opacas. Ele nao conhece Order, Payment, Customer ou qualquer outro
dominio do Nexus.

## Regras de fronteira

- Cada servico possui banco PostgreSQL, usuario, migrations e credenciais
  proprios. Uma instancia PostgreSQL local pode hospedar bancos logicos
  distintos, sem acesso entre eles.
- Integracoes usam HTTP/JSON e autenticacao tecnica. Nao compartilhar entities,
  DTOs internos, bibliotecas de dominio ou tabelas.
- No Nexus, a comunicacao passa por ports e adapters/ACL. O dominio de Order nao
  conhece HTTP, DummyPay ou Notification Service.
- Chaves de idempotencia e identificadores de referencia sao valores opacos
  definidos pelo chamador. Reenvios devem preservar a mesma chave.
- Os READMEs e ADRs dos repositorios dos servicos sao a fonte de verdade de seus
  contratos. Este documento registra apenas a fronteira do Nexus.

## DummyPay

DummyPay e um PSP autonomo para pagamentos de cartao por venda unica
(`auth_and_capture`). Ele aceita somente tokens de cenario, nunca PAN, CVV,
validade ou nome do portador.

O futuro Payment Service deve chamar `POST /v1/payments` com Basic Auth,
`Idempotency-Key`, `reference_id`, valor inteiro em centavos, moeda `BRL` e um
dos tokens documentados pelo PSP. A resposta pode ser `APPROVED`, `REJECTED` ou
`PROCESSING`.

Para pagamentos assincronos, o Payment Service registra previamente uma
assinatura em `POST /v1/webhook-subscriptions`. DummyPay envia eventos
`payment.processing`, `payment.approved` e `payment.rejected`, assinados por
HMAC-SHA-256 sobre os bytes brutos. A entrega pode ocorrer fora de ordem; o
consumidor deve tratar cada evento de forma idempotente. Falhas podem ser
reexecutadas pelo endpoint administrativo do PSP.

O Nexus nao deve chamar DummyPay diretamente. Essa regra preserva o Payment
Service como a fronteira para redundancia futura entre PSPs.

## Notification Service

Notification Service aceita `POST /v1/notifications` com Basic Auth e
`Idempotency-Key`. A chave de notificacao e derivada exclusivamente desse
cabecalho; o mesmo payload devolve a notificacao existente e uma reutilizacao
com payload diferente retorna `409`.

Os canais implementados sao `EMAIL` e `SMS`, ambos com providers fake. O
chamador envia `recipient`, `body`, `reference_id` e, quando necessario,
`callback_id` e `callback_name`; para e-mail tambem envia `subject`. Essas
referencias sao opacas e servem somente para correlacao. A criacao responde
`202 Accepted` em `PENDING`; `GET /v1/notifications/{notification_id}` permite
consultar o resultado posterior.

O servico processa a entrega por worker interno e lease, logo `202` nao
significa `SENT`. O Checkout registra a intencao imutavel em
`notification_submissions`, com payload e `Idempotency-Key` persistidos, antes
de chamar o servico. A reserva ocorre com a aprovacao do pedido; apos o commit,
Billing e Shipping executam antes do dispatch. Falhas remotas ficam no journal e
podem ser recuperadas ou descartadas pelo backoffice interno, sem expor dados da
mensagem na listagem.

## Sequencia de evolucao

1. Criar o Payment Service, com port para PSP e adapter HTTP para DummyPay.
2. Integrar e validar o fluxo Payment Service -> DummyPay, incluindo
   idempotencia, webhooks, timeout e reconciliacao.
3. ~~Refatorar o Nexus para substituir o provider de Payment local pelo adapter
   HTTP do Payment Service, preservando o contrato de checkout.~~ Feito.
4. ~~Extrair o consumo de notificacao para o Notification Service por adapter/ACL
   proprio, sem acoplamento ao dominio de notificacao generico.~~ Feito com o
   journal tecnico do Checkout e o backoffice de recuperacao.
