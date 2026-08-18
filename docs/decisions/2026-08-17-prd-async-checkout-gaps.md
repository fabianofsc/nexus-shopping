# Lacunas do checkout assincrono apos a integracao do monolito

Data: 2026-08-17
Status: aberto (follow-up consciente, sem mudanca de codigo neste lote)

## Contexto

A branch `monolith-first` trouxe o contexto Inventory e passou a reservar estoque dentro
da transacao que cria o pedido. A `main` ja havia extraido o Payment para o
`nexus-payment-service`, tornando o pagamento assincrono: o checkout responde `202` com o
pedido em `WAITING_PAYMENT` e o resultado terminal chega por reconciliacao.

A integracao das duas linhas resolveu o problema mais grave (o estoque nunca voltava numa
recusa, porque a liberacao vivia no caminho sincrono que deixou de ser alcancavel). A
liberacao passou para `PaymentReconciliationUseCase`.

Restam dois riscos que **nascem da combinacao** reserva-sincrona + pagamento-assincrono e
que foram deliberadamente deixados fora do lote de integracao, para manter o PR focado.

## Risco A: cancelamento durante `WAITING_PAYMENT`

O pedido agora fica em `WAITING_PAYMENT` por segundos (o DummyPay tem
`DUMMYPAY_PROCESSING_DELAY: 3s` e o scheduler roda a cada ~2s). Nessa janela o cliente
pode cancelar.

Sequencia:

1. `POST /customers/{id}/orders/{orderId}/cancel` -> `CancelOrderUseCase` aceita (o status
   era `WAITING_PAYMENT`) e libera o estoque via `ReleaseStockPort`.
2. O polling traz `APPROVED` do provider.
3. `ApplyOrderPaymentResultUseCase` chama `Order.applyPaymentResult(...)`, que lanca
   `OrderStateTransitionException` -> `OrderStateConflictException`.
4. `PaymentReconciliationUseCase.reconcile()` engole com
   `catch (exception: RuntimeException) { logger.warn(...) }`.

Resultado: cobranca capturada no PSP, pedido `CANCELLED` no Nexus, sem notificacao, e
apenas um WARN no log. Antes da extracao esse cenario nao existia, porque o pagamento era
sincrono e o pedido nunca ficava pendurado.

Encaminhamento sugerido: recusar o cancelamento enquanto existir `PaymentAttempt` em
`REQUESTED` para o pedido (responder `409`), e distinguir "conflito esperado" de "erro
real" no catch da reconciliacao, para nao perder o alerta de pagamento capturado sem
pedido correspondente.

## Risco B: falha do provider depois do commit

Em `ExecuteCheckoutUseCase`, `inventory.decrement(...)` e `carts.confirmCheckout(...)`
acontecem dentro de `transaction.inTransaction { ... }`, que **commita antes** do dispatch
HTTP. `PaymentServiceProviderGateway` lanca `PaymentProviderGatewayException` quando o
`nexus-payment-service` esta indisponivel.

Resultado com o servico fora do ar: o cliente recebe erro, mas o estoque ja foi baixado, o
carrinho ja foi fechado e o pedido fica orfao em `WAITING_PAYMENT` — sem attempt para a
reconciliacao varrer, logo sem quem devolva o estoque.

Encaminhamento sugerido: compensar no `catch` (liberar estoque e marcar o pedido) ou
introduzir expiracao por timeout para pedidos em `WAITING_PAYMENT` sem attempt terminal,
tratando-os como recusa.

## Decisao

Registrar os dois riscos e trata-los em lote proprio. A integracao do monolito na `main`
segue sem eles, com o build completo verde e a liberacao de estoque por reconciliacao
coberta por teste.
