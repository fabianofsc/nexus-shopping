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
`adapter`. Ele e a camada mais externa do fluxo e pode depender das portas de entrada
publicas de Cart e Order; nenhum dos dois contextos pode importar `integration`.

Cada contexto expoe uma porta de entrada pequena, implementada pelo seu caso de uso:

- Cart: obter e bloquear o carrinho ACTIVE do cliente para checkout e marca-lo como
  CHECKED_OUT.
- Order: criar um pedido a partir de dados que ja sao snapshots (cliente, endereco e
  itens), sem consultar Cart nem importar tipos de Cart.

O workflow de checkout define seus proprios gateways outbound para esses contratos.
Os adapters locais dos gateways traduzem os DTOs do workflow para as portas de entrada
de Cart e Order. A implementacao local participa de uma unica `TransactionPort`,
mantendo a atomicidade atual entre travar o carrinho, criar o pedido e marcar o
carrinho como CHECKED_OUT.

O controller HTTP de checkout tambem pertence a `integration/checkout/adapter/inbound/http`.
O endpoint, o header `Idempotency-Key`, os codigos 201/200 e o corpo de resposta
permanecem inalterados. `paymentToken` ainda nao entra nesta spec; ele sera introduzido
na spec de Payment.

## Contratos

Os nomes finais podem seguir a convencao Kotlin do repositorio, mas a separacao e
obrigatoria:

```text
CartCheckoutInputPort
  lockActiveCart(customerId) -> LockedCart
  markCheckedOut(cartId)

CreateOrderInputPort
  create(CreateOrderCommand) -> CreatedOrder

CheckoutCartGateway
  lockActiveCart(customerId) -> CheckoutCartData
  markCheckedOut(cartId)

OrderCreationGateway
  create(CreateOrderData) -> CheckoutOrderData
```

`LockedCart` e `CheckoutCartData` sao tipos de Cart/workflow; `CreateOrderCommand`
e `CheckoutOrderData` sao tipos de Order/workflow. A traducao ocorre nos adapters do
workflow. Nenhum contrato usa entidade JPA ou tipo de dominio do outro contexto.

## Regras e falhas

- Carrinho inexistente, vazio ou nao ACTIVE continua a falhar com o mesmo Problem
  Details atual.
- A mesma chave de idempotencia e o mesmo payload devolvem o pedido original; payload
  diferente devolve 409.
- Concorrencia continua a criar somente um pedido e a concluir somente um carrinho.
- Uma falha em qualquer etapa faz rollback local de Cart e Order.
- Nenhum tipo sob `cart/` importa `order/`; nenhum tipo sob `order/` importa `cart/`;
  ambos nao importam `integration/`.

## Testes de aceitacao

1. Teste de arquitetura que proibe imports de `cart` para `order`, de `order` para
   `cart` e de ambos para `integration`.
2. Testes unitarios dos adapters locais verificam a traducao de dados sem JPA ou HTTP.
3. Os testes HTTP existentes de checkout continuam verdes, incluindo replay e conflito
   de idempotencia.
4. Os testes de concorrencia e de rollback do checkout continuam verdes.
5. A busca por `CartCheckoutPort` nao retorna codigo de producao: a porta antiga e seu
   acoplamento sao removidos.

## Fora de escopo

- Comunicacao HTTP real entre contextos.
- Saga, outbox, broker ou consistencia eventual entre Cart e Order.
- Payment e Notification; eles dependem desta fronteira e pertencem a proxima spec.

## Criterio de conclusao

A spec esta concluida quando checkout conserva seu contrato e atomicidade locais, mas
Cart e Order podem ser compilados sem depender um do outro. O unico componente que
conhece ambos e `integration/checkout`.
