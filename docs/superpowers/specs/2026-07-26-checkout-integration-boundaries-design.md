# Spec: Fronteiras de integracao do Checkout

**Status:** Aprovada
**Data:** 2026-07-26
**Pre-requisito para:** `2026-07-26-payment-context-design.md`

## Objetivo

Remover o acoplamento atual em que `CartJpaRepositoryAdapter` implementa a porta
`CartCheckoutPort`, pertencente a `Order`. O checkout passa a ser um workflow de
integracao, fora dos Bounded Contexts, que usa portas de entrada e gateways
explicitos para coordenar Cart e Order sem que um contexto importe tipos do outro.

`Checkout` continua sendo um fluxo de aplicacao, nao um novo Bounded Context e nao
possui modelo de dominio proprio.

## Problema atual

`order/application/port/outbound/CartCheckoutPort.kt` contem uma visao de carrinho
em tipos de Order e e implementada por `cart/adapter/outbound/jpa/CartJpaRepositoryAdapter`.
Embora os snapshots evitem o vazamento de `Cart`, o adapter de Cart precisa importar
uma abstracao de Order. Isso prende a composicao local dos contextos e aumenta o custo
de extrair Cart ou Order em servicos separados.

## Desenho aprovado

Criar o modulo tecnico `integration/checkout`, organizado em `application` e
`adapter`. `integration/checkout/application` depende somente de seus gateways
outbound e da sua `TransactionPort`; ele nao conhece adapters concretos nem tipos de
Cart/Order. Os adapters locais em `integration/checkout/adapter/outbound` implementam
esses gateways e dependem das portas de entrada publicas de Cart e Order. Nenhum dos
dois contextos pode importar `integration`.

Cada contexto expoe uma porta de entrada pequena, implementada pelo seu caso de uso:

- Cart: obter e bloquear o carrinho ACTIVE do cliente para checkout e marca-lo como
  CHECKED_OUT.
- Order: criar um pedido a partir de dados que ja sao snapshots (cliente, endereco e
  itens), sem consultar Cart nem importar tipos de Cart.

O workflow de checkout define seus proprios gateways outbound e DTOs. Os adapters
locais desses gateways constituem uma Anti-Corruption Layer (ACL): traduzem os DTOs do
workflow para os request/response models das portas de entrada de Cart e Order. O
workflow nao importa casos de uso concretos.

`integration/checkout/application` e dono da sua `TransactionPort`. O adapter Spring
JPA tecnico fica em `integration/checkout/adapter/outbound` e abre uma unica transacao
local com propagacao `REQUIRED` para `reservar carrinho -> criar pedido -> concluir
carrinho`. As operacoes de Cart e Order participam da transacao existente, sem iniciar
ou encerrar uma fronteira adicional. Isto preserva a atomicidade no monolito; nao e
uma transacao distribuida e sera substituido por reserva/compensacao quando um desses
contextos for remoto.

O controller HTTP de checkout tambem pertence a `integration/checkout/adapter/inbound/http`.
O endpoint, o header `Idempotency-Key` e o corpo `OrderResponse` permanecem
inalterados. Sem Payment, a criacao/replay usam 201/200 como hoje. A spec de Payment
estende o mesmo endpoint com 202 Accepted quando a tentativa reservada ainda esta
`REQUESTED`: o corpo continua sendo `OrderResponse` em `WAITING_PAYMENT`, nenhum
resultado de Payment e aplicado e o mesmo replay reconcilia o resultado terminal.
`paymentToken` ainda nao entra nesta spec; ele sera introduzido na spec de Payment.

## Contratos

Os nomes finais podem seguir a convencao Kotlin do repositorio, mas ownership e
direcao de dependencias sao obrigatorios:

```text
cart/application/port/inbound/CartCheckoutInputPort
  reserveActiveCart(customerId) -> CartCheckoutReservation
  confirmCheckout(reservationId)

order/application/port/inbound/CreateOrderInputPort
  create(CreateOrderCommand) -> CreatedOrder

integration/checkout/application/port/outbound/CheckoutCartGateway
  reserveActiveCart(customerId) -> CheckoutCartData
  confirmCheckout(reservationId)

integration/checkout/application/port/outbound/OrderCreationGateway
  create(CreateOrderData) -> CheckoutOrderData(replayed)

integration/checkout/application/port/outbound/OrderPaymentResultGateway
  apply(OrderPaymentResultData) -> CheckoutOrderData

integration/checkout/application/port/outbound/PaymentValidationGateway
  validate(PaymentValidationData)

integration/checkout/application/port/outbound/PaymentProcessingGateway
  process(PaymentProcessingData) -> PaymentProcessingData

integration/checkout/application/port/outbound/NotificationGateway
  ensure(NotificationData)

integration/checkout/application/port/outbound/TransactionPort
  inTransaction(block)
```

`CartCheckoutReservation` e os request/response models de Cart pertencem a Cart;
`CreateOrderCommand` e `CreatedOrder` pertencem a Order. `CheckoutCartData`,
`CreateOrderData`, `CheckoutOrderData`, `OrderPaymentResultData`,
`PaymentValidationData`, `PaymentProcessingData` e `NotificationData` pertencem a
Integration. A traducao ocorre somente nos adapters locais da ACL. Nenhum gateway,
DTO ou contrato do workflow usa entidade JPA ou tipo de dominio de Cart, Order,
Payment ou Notification.

`CheckoutOrderData` inclui `orderReference`, `status`, `total`, `recipientEmail` e
`replayed`. Order continua sendo autoridade da idempotencia de criacao; o workflow
apenas propaga a chave e usa `replayed` para impedir efeitos posteriores duplicados.
Etapas posteriores idempotentes ainda podem ser chamadas para reconciliar estado, como
sera definido pela spec de Payment.

O workflow nao tem entidades, schema, tabelas, repositorios, regras de preco, regras
de carrinho ou regras de transicao de pedido. Ele apenas orquestra, traduz contratos e
controla a fronteira transacional local.

## Regras e falhas

- Carrinho inexistente, vazio ou nao ACTIVE continua a falhar com o mesmo Problem
  Details atual.
- A mesma chave de idempotencia e o mesmo payload devolvem o pedido original; payload
  diferente devolve 409.
- Concorrencia continua a criar somente um pedido e a concluir somente um carrinho.
- Uma falha em qualquer etapa faz rollback local de Cart e Order.
- A reserva e a confirmacao sao a semantica de negocio de Cart. Nesta primeira etapa,
  o adapter local pode implementa-las com lock pessimista; um Cart remoto exigira uma
  reserva com validade e possivel compensacao, nao um lock de linha atraves da rede.
- Nenhum tipo sob `cart/` depende de `order` ou `integration`; nenhum tipo sob
  `order/` depende de `cart` ou `integration`.

## Testes de aceitacao

1. Teste de arquitetura por dependencias de classe/pacote, nao por busca textual, que
   proibe `cart.. -> order..|integration..` e `order.. -> cart..|integration..`.
   `integration/checkout/application..` tambem nao depende de
   `integration/checkout/adapter..`, `cart..`, `order..`, `payment..` ou
   `notification..`; somente adapters sob `integration..` podem depender de portas
   inbound dos contextos autorizados.
2. Testes unitarios dos adapters locais verificam a ACL e a traducao sem JPA ou HTTP.
3. Os testes HTTP existentes de checkout continuam verdes, incluindo replay e conflito
   de idempotencia; a extensao de Payment cobre 202 com `OrderResponse` em
   WAITING_PAYMENT sem alterar o contrato dos demais cenarios.
4. Um teste de integracao força falha apos a reserva e prova no banco o rollback de
   Cart e Order; os testes existentes de concorrencia continuam verdes.
5. A busca por `CartCheckoutPort` nao retorna codigo de producao: a porta antiga e seu
   acoplamento sao removidos.
6. Um teste de composicao prova que somente o root/configuracao de Integration conhece
   os adapters concretos; Cart e Order dependem apenas de suas proprias portas.

## Fora de escopo

- Comunicacao HTTP real entre contextos.
- Saga, outbox, broker ou consistencia eventual entre Cart e Order.
- Payment e Notification; eles dependem desta fronteira e pertencem a proxima spec.

## Criterio de conclusao

A spec esta concluida quando checkout conserva seu contrato e atomicidade locais, mas
Cart e Order podem ser compilados sem depender um do outro. O unico componente que
conhece ambos e `integration/checkout`. Esta entrega isola dependencias de codigo; a
FK atual `orders.cart_id` continua uma limitacao deliberada do schema compartilhado e
sua remocao pertence a futura extracao para bancos independentes.
