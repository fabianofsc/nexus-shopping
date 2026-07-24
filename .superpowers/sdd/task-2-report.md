# Task 2 - Persistencia e transacao de checkout

## Entrega

- Adicionada a migration portavel `V8__create_order_context.sql` com `orders` e
  `order_items`, snapshots historicos, `UNIQUE(cart_id)`,
  `UNIQUE(customer_id, idempotency_key)` e indice de listagem por cliente/data.
- Implementados `OrderEntity`, `OrderItemEntity`, repositorio Spring Data com JPQL
  explicito e `OrderJpaRepositoryAdapter`, incluindo leitura, `Slice` sem
  `COUNT(*)`, create-if-absent e atualizacao de cancelamento.
- Introduzidas `TransactionPort` e `CartCheckoutPort`. O adapter JPA bloqueia o
  carrinho ACTIVE, devolve itens como `OrderItemSnapshot` e o marca CHECKED_OUT.
- Checkout agora recebe apenas customer e snapshots: consulta replay antes do
  carrinho, compara fingerprint somente do payload confiavel, bloqueia/reconsulta,
  cria o pedido com snapshot e fecha o carrinho na mesma transacao.
- Ajustado o contrato legado de migration de catalogo para analisar apenas V1-V3;
  ele nao deve proibir `UNIQUE` de contextos posteriores.

## RED / GREEN observado

1. `OrderMigrationContractTest` ficou RED sem a V8 (`orders` inexistente) e ficou
   GREEN apos a migration.
2. `CheckoutOrderTransactionUseCaseTest` ficou RED por portas e construtor ausentes
   e ficou GREEN apos introduzir as portas, o fluxo transacional e o fingerprint
   sem cart/itens.
3. `OrderJpaRepositoryAdapterTest` ficou RED por adapter JPA ausente e ficou GREEN
   apos entidades, JPQL e adapter.
4. `CheckoutOrderConcurrencyTest` foi executado em H2 real e ficou GREEN: 12
   requisicoes com a mesma chave retornaram um pedido, e 12 chaves distintas
   permitiram somente um checkout. Esta cobertura foi adicionada na integracao do
   adapter e sua primeira execucao ja foi GREEN; portanto nao ha RED isolado para
   esse teste adicional.

## Verificacao executada

- `./gradlew ktlintCheck`: GREEN.
- Suite focada de Order (migration, use cases, adapter e concorrencia H2): GREEN.
- `CatalogMigrationContractTest`: inicialmente RED porque lia todas as migrations e
  proibia qualquer `UNIQUE`; apos restringir o escopo ao catalogo V1-V3, GREEN.

## Limitacao conhecida de verificacao

O primeiro `./gradlew build --no-parallel` parou no
`CatalogMigrationContractTest`, corrigido acima. A repeticao integral foi iniciada,
mas interrompida externamente enquanto `:test` ainda executava. Portanto este
relatorio nao afirma que o build completo final tenha passado; a verificacao
integral independente permanece pendente.

## Self-review

- Nenhum controller/DTO HTTP foi adicionado.
- Domain/application continuam sem imports JPA, Hibernate, Spring Data ou HTTP.
- A atualizacao de add/remove/clear do carrinho foi preservada para a Task 3;
  somente os metodos de checkout foram acrescentados ao adapter.
- Os snapshots do pedido sao persistidos em `orders`/`order_items` e nao dependem
  de entidades vivas de produto ou carrinho.
