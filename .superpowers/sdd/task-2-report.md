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

## Correcoes transversais da revisao final

- O use case agora valida antes da transacao/chamada ao adapter: chave com 1..255
  caracteres, campos obrigatorios e limites de cada snapshot conforme V8. Tambem
  valida ids, nome/tamanho, preco `NUMERIC(12, 2)` e quantidade dos itens bloqueados.
  Contratos unitarios e HTTP cobrem 400 para chave longa e snapshots invalidos.
- O adapter JPA nao captura mais `DataIntegrityViolationException` apos
  `saveAndFlush`. O replay e resolvido antes do insert, pelo lookup e pelo lock
  pessimista do carrinho com recheck na mesma transacao. Isso e portavel entre H2 e
  PostgreSQL e evita consultar uma transacao PostgreSQL ja abortada; nao usa SQL
  especifico nem `REQUIRES_NEW`.
- A query do ultimo carrinho para mutacao agora retorna `Slice` com `PageRequest`
  de uma linha, sem materializar historico do cliente. O contrato de migration cria
  o segundo cart antes de provar a `UNIQUE(customer_id, idempotency_key)`.

### Evidencia RED / GREEN adicional

1. RED: `CheckoutOrderTransactionUseCaseTest` falhou para chave >255, campo
   obrigatorio vazio, snapshot acima do limite e item invalido. GREEN apos a
   validacao pura do payload e dos itens bloqueados. `OrderControllerTest` tambem
   ficou GREEN para os Problem Details 400 correspondentes.
2. RED: `CartJpaRepositoryAdapterTest` nao compilava porque a query de ultimo
   carrinho ainda retornava `List`; GREEN apos usar `Slice` e `PageRequest.of(0, 1)`.
3. RED: `OrderJpaRepositoryAdapterTest` mostrou que duplicidade era recuperada
   dentro da transacao; GREEN apos propagar a violacao, deixando o replay para o
   lock/recheck do checkout.
4. A nova regressao concorrente cobre 12 replays da chave original apos a criacao
   de um novo cart ACTIVE. Ela ficou GREEN na primeira execucao, pois a protecao de
   replay ja existia; este fato e registrado sem inventar um RED. A ressalva
   historica do primeiro teste de concorrencia permanece valida.

## Achado final: NUMERIC e nomenclatura de criacao

- RED: `BigDecimal("99999999999")` era aceito porque a validacao usava apenas
  `precision() <= 12`; GREEN: a validacao agora usa `stripTrailingZeros()`, aceita
  `1.230` (escala efetiva 2), rejeita escala efetiva maior que 2, mais de 10
  digitos inteiros e o caso de escala negativa `1E+11`.
- RED: os fakes e testes de adapter nao compilavam contra o contrato desejado de
  criacao simples; GREEN: `createIfAbsent...`/`OrderCreationResult.created` foram
  substituidos por `create` e `CheckoutOrderResult.replayed`. O adapter apenas
  insere ou propaga a violacao; o use case decide replay antes do insert sob lock.
