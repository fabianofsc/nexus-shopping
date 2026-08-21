# Spec: Integracao HTTP do Notification Service com recuperacao manual

**Status:** Proposta aprovada para documentacao
**Data:** 2026-08-16
**Decisao:** `docs/decisions/2026-08-16-prd-notification-service-http-integration.md`

## Objetivo

Substituir o bounded context local `notification` por uma integracao HTTP
sincrona de aceite com o Notification Service. A entrega permanece assincrona no
servico externo. Falha de notificacao nao falha checkout; uma submissao local
duravel permite diagnostico e retry ou descarte manual no Nexus.

## Escopo

- Preservar `NotificationGateway` como porta do Checkout.
- Criar ACL HTTP com Basic Auth, `Idempotency-Key` e mapeamento de `202`.
- Introduzir `NotificationSubmission` tecnico em `checkout`.
- Criar backoffice para listar, retry e discard de submissao.
- Remover todo o bounded context local `notification` e a API `/notifications`.
- Atualizar compose e documentacao para declarar o Notification Service como
  dependencia de runtime.

## Fora de escopo

- Polling do estado de entrega, webhooks ou exibicao do status final ao cliente.
- Retry automatico, scheduler, broker ou fila externa.
- Migracao de historico local para o servico externo.
- Autenticacao/autorizacao dentro do processo Spring. A topologia de rede protege
  o backoffice nesta entrega.

## Arquitetura

`checkout/application` continua dependendo somente de portas. O
workflow pede a confirmacao de notificacao por `NotificationGateway`, cuja
implementacao combina o journal local com um client HTTP privado do adapter.

```text
ExecuteCheckoutUseCase / PaymentReconciliationUseCase
  -> TransactionPort: aplicar aprovacao + reservar NotificationSubmission
  -> commit
  -> Billing: emitir Invoice
  -> Shipping: calcular e despachar
  -> NotificationGateway.dispatch
       -> claim da submissao
       -> Notification Service HTTP POST
       -> ACCEPTED ou FAILED

Backoffice HTTP
  -> use case de consulta/retry/discard
  -> NotificationSubmissionRepositoryPort
  -> NotificationGateway para a tentativa manual
```

O HTTP fica sempre apos o commit. A reserva do journal usa a mesma transacao que
registra a transicao de pagamento aprovada; portanto uma aprovacao persistida nao
perde sua intencao de notificacao se o processo cair antes da chamada remota.

Billing e Shipping sao efeitos posteriores ja existentes e permanecem entre o
commit e o dispatch. Assim, para uma aprovacao efetiva, a ordem e
`Billing -> Shipping -> Notification`. A reserva ja existe se Billing ou Shipping
falharem, mas o dispatch nao ocorre e a submissao fica `PENDING`; esses erros
continuam interrompendo a sequencia conforme sua propria spec. Somente a falha da
tentativa de notificacao e absorvida pelo `NotificationGateway`, marcada como
`FAILED` e incapaz de alterar a resposta confirmada do checkout.

## Modelo e regras

`NotificationSubmission` guarda o payload imutavel, a chave idempotente, a
correlacao de pedido/tentativa, estado, numero de tentativas, ultimo erro
sanitizado, ID remoto e lease de envio. Estados validos: `PENDING`, `IN_FLIGHT`,
`ACCEPTED`, `FAILED`, `DISCARDED`.

- Reserva por pedido+tentativa e idempotente.
- Uma tentativa reclama somente `PENDING`, `FAILED` ou lease expirada.
- A conclusao exige o mesmo token de lease.
- Apenas `202` remoto resulta em `ACCEPTED`.
- Qualquer erro remoto ou de transporte nao e propagado ao checkout.
- Retry reutiliza dados persistidos, jamais re-renderiza a mensagem.
- Discard exige justificativa e e terminal.

## Contrato HTTP externo

O adapter envia `POST /v1/notifications` com Basic Auth e:

```json
{
  "channel": "EMAIL",
  "recipient": { "email": "cliente@example.com" },
  "subject": "Pedido 123 confirmado",
  "body": "Seu pedido 123 no valor de 99.90 foi confirmado.",
  "reference_id": "order:123",
  "callback_id": "order:123",
  "callback_name": "order_confirmed"
}
```

`Idempotency-Key` e `order-confirmed:123:attempt-reference`. O adapter mapeia o
`notification_id` de `202` ao journal e nunca comunica `PENDING` como `SENT`.

## Backoffice

```text
GET  /backoffice/notification-submissions?status=&page=&size=
POST /backoffice/notification-submissions/{id}/retry
POST /backoffice/notification-submissions/{id}/discard
```

As respostas seguem o formato existente de slice e Problem Details RFC 7807. A
listagem omite o payload e dados pessoais; os comandos retornam o resumo
operacional atualizado. Os endpoints sao internos por topologia de rede,
consistente com a ausencia atual de autenticacao na aplicacao.

## Testes de aceitacao

1. O adapter HTTP envia header Basic Auth, chave idempotente e corpo exato; `202`
   armazena ID remoto e marca `ACCEPTED`.
2. Timeout, conexao recusada, `429`, `5xx` e `4xx` marcam `FAILED` sem transformar
   checkout aprovado em falha HTTP.
3. Aplicar aprovacao e reservar submissao sao atomicos, no checkout sincrono e na
   reconciliacao de pagamentos.
4. Depois do commit, Billing antecede Shipping e ambos antecedem o dispatch; uma
   falha em Billing ou Shipping preserva a submissao `PENDING` e impede a chamada
   HTTP nessa execucao.
5. Retry usa o mesmo corpo/chave, respeita lease e nao reenvia `ACCEPTED` ou
   `DISCARDED`; discard exige justificativa e impede retry futuro.
6. Backoffice lista apenas dados operacionais e segue a paginacao slice.
7. Testes de arquitetura provam que os pacotes antigos `notification` nao existem
   e que o dominio/aplicacao nao importam Spring, JPA ou tipos HTTP.
8. Migrations permanecem portaveis entre PostgreSQL e H2; WireMock cobre a
   integracao offline.
