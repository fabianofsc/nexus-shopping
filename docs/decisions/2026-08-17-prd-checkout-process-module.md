# ADR: Nomear Checkout como processo de aplicacao intercontextual

**Status:** Aceita
**Data:** 2026-08-17

## Contexto

O modulo `integration/checkout` coordenava Cart, Customer, Order, Payment,
Inventory e Notification por gateways e ACLs. A direcao de dependencias ja era
hexagonal, mas o nome tecnico `integration` aparecia ao lado dos Bounded Contexts
e induzia a leitura de que era um dominio.

Checkout nao possui entidade, agregado, estado ou invariantes proprios. Os estados
do pedido pertencem a Order, e as tentativas e resultados de pagamento pertencem a
Payment.

## Decisao

O modulo passa a se chamar `checkout/` e representa um processo de aplicacao
intercontextual. Ele mantem somente `application/` e `adapter/`; nao tera
`domain/` e nao e um Bounded Context.

A aplicacao expoe `ExecuteCheckoutInputPort`. O caso de uso e puro e a configuracao
Spring fica em `checkout/adapter/config`. ACLs outbound sao a unica fronteira que
conhece as portas inbound publicas dos Bounded Contexts.

## Consequencias

- A estrutura passa a comunicar a capacidade de negocio sem atribuir ownership de
  dominio inexistente a Checkout.
- Regras ArchUnit impedem contextos de dependerem de Checkout e impedem a aplicacao
  de Checkout de depender de contextos, adapters ou frameworks.
- Uma futura reconciliacao de pagamentos de checkout sera outro caso de uso desse
  processo. Payment continuara dono das tentativas, do provider e do resultado.

As specs e planos historicos que citam `integration/checkout` descrevem o nome
vigente na epoca; esta ADR os sucede quanto a nomenclatura e estrutura atuais.
