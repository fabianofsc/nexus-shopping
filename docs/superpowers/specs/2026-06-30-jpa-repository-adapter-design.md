# Refactor do Adapter de Repository para JPA

Status: aprovado para planejamento. Nao implementar ate confirmacao explicita do usuario.

## Contexto

O projeto usa arquitetura hexagonal para manter a direcao de dependencia `adapters -> application -> domain`. O dominio de produto e os use cases dependem apenas de `ProductRepositoryPort`; hoje a implementacao outbound usa `JdbcTemplate` e `SimpleJdbcInsert` em `product/adapter/outbound/jdbc/ProductRepository.kt`.

A mudanca proposta substitui esse adapter por uma implementacao com JPA e Spring Data, preservando o contrato de aplicacao e o comportamento publico dos endpoints. A motivacao e didatica: mostrar uma alternativa ORM sem abandonar a clareza das consultas de leitura que sustentam as licoes de performance do projeto.

## Decisoes Aprovadas

- O dominio continua puro. `Product` e `ProductPage` nao recebem anotacoes JPA nem imports de framework.
- O adapter outbound passa a ter entidade de persistencia separada, `ProductEntity`, dentro de um pacote JPA.
- As consultas de leitura usam `@Query` com JPQL explicito para manter o foco didatico no shape das queries.
- Operacoes de escrita seguem o padrao natural do JPA: `save`, entidade gerenciada e dirty checking.
- `brand_id` e `category_id` serao mapeados como campos escalares (`Long`) em `ProductEntity`, sem criar `BrandEntity` ou `CategoryEntity`.
- A paginacao preserva a semantica atual de `page * size` e `hasNext`, evitando `COUNT(*)`. No JPA, isso sera feito com `Slice` e `PageRequest.of(page, size)`, deixando o Spring Data buscar a linha extra necessaria para detectar proxima pagina.
- O comportamento publico dos endpoints deve permanecer igual.

## Arquitetura Proposta

A mudanca fica restrita ao adapter outbound. A nova estrutura sera:

```text
src/main/kotlin/com/nexus/shopping/product/
  adapter/
    outbound/
      jpa/
        ProductEntity.kt
        SpringDataProductRepository.kt
        ProductJpaRepositoryAdapter.kt
```

`ProductEntity` representa a tabela `products`. Ela conhece nomes de colunas, estrategia de id e timestamps. Ela nao substitui `Product`; apenas traduz o modelo relacional para o adapter.

`SpringDataProductRepository` estende `JpaRepository<ProductEntity, Long>` e contem os metodos de leitura anotados com `@Query`. Ele e um detalhe interno do adapter.

`ProductJpaRepositoryAdapter` implementa `ProductRepositoryPort`, calcula parametros auxiliares como `upperBound` para busca por prefixo, monta `PageRequest`, chama o repository Spring Data e converte `Slice<ProductEntity>` para `ProductPage`.

## Consultas

As leituras usam JPQL explicito:

```kotlin
@Query(
    """
    SELECT p FROM ProductEntity p
    WHERE p.categoryId = :categoryId
    ORDER BY p.id
    """,
)
fun findByCategoryId(categoryId: Long, pageable: Pageable): Slice<ProductEntity>
```

```kotlin
@Query(
    """
    SELECT p FROM ProductEntity p
    WHERE p.name >= :name
      AND p.name < :upperBound
      AND p.name LIKE :prefix
    ORDER BY p.name
    """,
)
fun findByNamePrefix(
    name: String,
    upperBound: String,
    prefix: String,
    pageable: Pageable,
): Slice<ProductEntity>
```

A busca por nome preserva a tecnica atual de prefix range bounds, permitindo que o indice B-tree em `name` continue relevante para o exemplo de performance.

## Escritas

`save(command)` cria uma `ProductEntity` nova, chama o repository Spring Data e retorna o dominio convertido. Defaults como `status`, `currency` e `inventoryQuantity` continuam vindo do comando validado pelo use case.

`updatePrice(id, priceAmount)` busca a entidade por id. Se nao existir, retorna `null`. Se existir, altera `priceAmount` na entidade gerenciada e deixa o JPA persistir a alteracao dentro de uma transacao. O retorno e o produto convertido depois da persistencia.

Para timestamps, o desenho usa lifecycle JPA/Hibernate para manter a escrita idiomatica. `createdAt` e preenchido no insert e `updatedAt` e atualizado no update. A spec aceita o uso de anotacoes Hibernate, como `@CreationTimestamp` e `@UpdateTimestamp`, desde que o contrato publico continue retornando timestamps preenchidos e as migrations continuem como fonte do schema.

## Tratamento de Erros

O adapter nao deve transformar erros de infraestrutura em excecoes de dominio genericas. O comportamento atual de erro HTTP sera preservado pelo `ProductExceptionHandler`: validacoes continuam nos use cases; falhas inesperadas de persistencia continuam virando resposta generica `500` sem detalhes internos.

O caso de FK inexistente no POST deve continuar retornando `500` com `Unexpected server error.` e sem vazar classes JDBC, JPA, Hibernate ou detalhes de constraint no body.

## Testes

Os testes unitarios existentes dos use cases devem continuar inalterados porque dependem apenas de `ProductRepositoryPort`.

Sera criado um teste de adapter JPA com H2 e Flyway cobrindo:

- busca por categoria com `Slice`, preservando offset `page * size` e `hasNext`;
- busca por nome com prefix range bounds;
- criacao de produto com retorno de id e defaults;
- atualizacao de preco existente;
- retorno `null` para atualizacao de id inexistente.

O `ProductControllerTest` continua sendo o contrato HTTP de integracao. Ele deve passar sem mudancas de expectativa. O helper que verifica vazamento de detalhes internos pode ser ampliado para cobrir nomes de excecoes JPA/Hibernate.

## Documentacao

`README.md` e `AGENTS.md` devem ser atualizados para refletir o novo adapter JPA. As referencias a `JdbcTemplate` como stack atual, ao pacote `adapter/outbound/jdbc` e a decisao antiga de rejeitar JPA/ORM devem ser substituidas por uma explicacao coerente com a nova direcao: dominio puro, port preservado, entidade JPA isolada no adapter e consultas de leitura didaticas com `@Query` JPQL.

Como `AGENTS.md` tem limite operacional no projeto, a atualizacao deve manter o arquivo com menos de 200 linhas.

## Criterios de Aceite

- `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build` passa.
- Nenhum import de JPA, Hibernate ou Spring Data entra em `domain` ou `application`.
- Leituras do adapter usam `@Query` JPQL.
- Escritas usam padrao JPA natural.
- O adapter novo nao usa `JdbcTemplate`.
- Controllers e use cases preservam comportamento publico.
- Migrations continuam portaveis entre PostgreSQL e H2.
- `README.md` e `AGENTS.md` descrevem o adapter JPA atual sem referencias contraditorias ao adapter JDBC.
- `AGENTS.md` permanece abaixo de 200 linhas.

## Fora de Escopo

- Criar entidades JPA para `brands` e `categories`.
- Trocar validacoes dos use cases por Bean Validation.
- Alterar contratos HTTP.
- Fazer novo benchmark JMeter.
- Remover ou alterar as migrations existentes, salvo se a implementacao revelar necessidade real de compatibilidade.
