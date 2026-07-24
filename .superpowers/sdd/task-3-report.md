# Task 3 - API HTTP e integracao com Cart

## Escopo entregue

- Checkout em `POST /customers/{customerId}/cart/checkout`, com snapshots no body e
  `Idempotency-Key` obrigatorio no header.
- Resposta 201 para criacao e 200 para replay idempotente do mesmo pedido.
- `OrderController` com detalhe, listagem paginada por Slice e cancelamento com ownership
  garantido pelo customer da rota.
- Problem Details 409 para conflito de chave idempotente e transicao de estado invalida.
- Mutacoes de item do carrinho passam a validar `ACTIVE` dentro da operacao protegida por lock,
  impedindo add, remove e clear depois do checkout.
- Nenhuma integracao com Payment ou Notification foi adicionada.

## TDD

Os seguintes ciclos RED/GREEN foram executados na suite
`com.nexus.shopping.order.OrderControllerTest`:

1. checkout criado: RED 404, GREEN 201;
2. replay: RED 201, GREEN 200;
3. chave reutilizada com payload divergente: RED, GREEN 409;
4. detalhe, ownership, listagem e cancelamento: RED por rota ausente/comportamento incorreto,
   GREEN apos a implementacao;
5. cancelamento de estado incompativel: RED, GREEN 409;
6. add, remove e clear apos checkout: RED com customers validos retornando 200, GREEN 400.

Tambem foram cobertos header ausente, carrinho vazio e carrinho ja fechado.

## Verificacao

- `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests 'com.nexus.shopping.cart.*' --tests com.nexus.shopping.order.OrderControllerTest`
  - verde.
- `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintCheck`
  - verde.
- `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build --no-parallel --console=plain`
  - concluido sem erro reportado.
- `rtk git diff --check`
  - sem problemas de whitespace.
- Busca restrita ao contexto Order por `payment|notification` encontrou apenas referencias de
  texto em nomes de testes/estados; nao ha dependencia ou integracao de Payment/Notification.

## Self-review

- DTO de checkout nao aceita `cartId` nem itens e converte para command por `toCommand()`.
- Application/domain continuam sem dependencias HTTP ou JPA adicionadas nesta task.
- O contrato anterior de `CheckoutOrderUseCase.execute(): Order` foi preservado; o novo
  `executeWithResult()` expoe a distincao criada/replay necessaria ao adapter HTTP.

## Complemento da revisao

### P1 - Paginacao

- RED: tres contratos HTTP invalidos retornaram erro diferente de Problem Details 400 e o teste
  unitario observou `IllegalArgumentException` do `PageRequest`.
- GREEN: `ListOrdersByCustomerUseCase` rejeita `page < 0` e `size` fora de `1..500` com
  `OrderValidationException`. Os tres contratos HTTP retornam 400.

### P1 - Cancelamento concorrente

- RED: duas requisicoes HTTP simultaneas nao atenderam a assercao de exatamente um 200 e um 409.
- GREEN: o cancelamento agora abre uma transacao unica, le o pedido com
  `PESSIMISTIC_WRITE`, valida a transicao e persiste antes de liberar o lock. O contrato HTTP
  concorrente retorna exatamente 200 e 409.

### P2 - Checkout contra mutacao

- Foi adicionada uma integracao H2 real para add, remove e clear. Ela bloqueia o checkout depois
  de adquirir o lock do carrinho e prova que cada mutacao ja leu o snapshot ACTIVE antes de
  disputar esse lock; apos o checkout, cada uma falha com `CartValidationException`, ha um unico
  pedido, o carrinho fica CHECKED_OUT e seus itens nao sao alterados.
- A protecao de validacao dentro de `updateCart` ja estava presente da entrega anterior; por isso
  este teste de regressao ficou verde na primeira execucao, sem introduzir um defeito artificial.

### P2 - Preparacoes explicitas

- Os fluxos de `OrderControllerTest` agora usam helpers que exigem 200 para add e 201 para o
  primeiro checkout antes de qualquer assercao posterior, eliminando os falsos verdes apontados.

### Verificacao do complemento

- Suite focada de `OrderControllerTest`, `OrderUseCasesTest` e
  `CheckoutOrderMutationConcurrencyTest`: verde.
- `ktlintCheck`: verde.
