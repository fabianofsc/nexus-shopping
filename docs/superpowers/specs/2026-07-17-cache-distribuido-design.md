# Spec de Evolucao da Fixture - v3.4-cache-distribuido

## Contexto

Repo: `/Users/fabiano/Developer/nexus-shopping` (`github.com/fabianofsc/nexus-shopping`).
Stack: Kotlin, Java 21, Gradle Wrapper, Spring Boot 4.1, PostgreSQL, arquitetura hexagonal.
Ponto de partida: tag `v3.3-cache-aside`.

Estado inicial:
- `CachingProductRepositoryAdapter` e um decorator explicito com Caffeine local, `@Primary`, fazendo cache-aside apenas em `findById`.
- `updatePrice` invalida somente a chave local.
- `findByCategoryId`, `findByName` e `save` sao pass-through.
- `ProductCacheProperties` usa `nexus.cache.product.max-size` e `nexus.cache.product.ttl`.
- Endpoints existentes: `GET /products/{id}`, `GET /products?categoryId=&name=&page=&size=`, `PATCH /products/{id}`.

## Objetivo

Evoluir a fixture de cache local para cache distribuido com Redis, depois adotar Spring Cache como atalho de framework, e por fim cachear buscas paginadas com TTL curto. O estado final deve ser correto e demonstravel como tag `v3.4-cache-distribuido`.

## Commits obrigatorios

### Commit 1 - Redis explicito

- Adicionar `spring-boot-starter-data-redis`.
- Remover Caffeine.
- Adicionar servico `redis` ao `docker-compose.yml`.
- Configurar `spring.data.redis.host` e `spring.data.redis.port` em `application.yml`.
- Trocar o cache-aside explicito no `CachingProductRepositoryAdapter` para Redis:
  - `GET` no Redis pela chave do produto.
  - Miss: buscar no delegate.
  - Produto encontrado: gravar no Redis com TTL.
  - `updatePrice`: delegar ao JPA e deletar a chave no Redis.
- Configurar o Redis explicito com chave `StringRedisSerializer` e valor `GenericJackson2JsonRedisSerializer`, para os valores ja ficarem legiveis em JSON no Redis.
- O cache deve ser compartilhado entre instancias.

### Commit 2 - Spring Cache para detalhe

- Adicionar `spring-boot-starter-cache`.
- Habilitar cache com `@EnableCaching`.
- Configurar `RedisCacheManager` usando `GenericJackson2JsonRedisSerializer`, nunca serializer JDK.
- Configurar TTL por cache.
- Desabilitar cache de `null` e/ou usar `unless = "#result == null"` para nao cachear ausencias.
- Remover decorator explicito.
- Anotar `ProductJpaRepositoryAdapter.findById` com `@Cacheable`.
- Anotar `ProductJpaRepositoryAdapter.updatePrice` para invalidar o detalhe pela chave `id` e invalidar todas as buscas cacheadas.
- Anotar `ProductJpaRepositoryAdapter.save` para invalidar todas as buscas cacheadas.
- Manter use cases cache-unaware e chamando o port por fora, para o proxy Spring interceptar.

### Commit 3 - Cache da busca paginada

- Adicionar `@Cacheable` em `ProductJpaRepositoryAdapter.findByCategoryId` e `ProductJpaRepositoryAdapter.findByName`.
- Usar cache separado para busca paginada.
- TTL da busca deve ser curto, exemplo `30s`.
- Chave da busca deve conter todos os parametros:
  - `findByCategoryId`: `categoryId`, `page`, `size`.
  - `findByName`: `name`, `page`, `size`.
- Atualizar `REFERENCE_POINTS.md` com a entrada `v3.4-cache-distribuido`, proposito, mudancas, como clonar, rodape `Tags ativas` e `Ultima atualizacao`.

## Guardrails

- O estado final deve estar correto: cache funcionando de verdade e provado por teste.
- Redis e a unica store de cache no estado final.
- Sem Caffeine no estado final.
- Sem decorator explicito no estado final.
- Sem cache multinivel.
- Serializacao Redis em JSON legivel com `GenericJackson2JsonRedisSerializer`.
- TTL por cache: detalhe de produto mais longo; busca curta; sem TTL infinito.
- Dominio e use cases permanecem cache-unaware.
- Contrato dos endpoints inalterado.
- Load-tests intocados.
- Diff minimo por commit.
- Seguir naming e layering existentes.
- `domain/` e `application/` sem imports de `jakarta.persistence`, `org.hibernate` ou `org.springframework.data`.
- Validacao vive no use case; adapter nao valida.
- Leituras JPA com `@Query` JPQL explicito; sem derived queries.
- Paginacao via `Slice` sem `COUNT(*)`.
- Usar Gradle wrapper com `GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local`.

## Definicao de pronto

- `./gradlew build` verde.
- `@Cacheable` comprovado: segundo `findById` identico nao bate no banco.
- `@CacheEvict` comprovado: apos `updatePrice`, proximo `findById` reflete o preco novo.
- Busca cacheada comprovada: query identica repetida dentro do TTL nao bate no banco.
- Parametros diferentes de busca geram entradas diferentes.
- `save` e `updatePrice` invalidam o cache de busca para evitar resposta obsoleta alem da janela tecnica minima.
- Coerencia distribuida documentada por verificacao manual com multiplas instancias e Redis compartilhado.
- Valores no Redis sao JSON legivel.
- Testes automatizados nao dependem de Redis previamente ligado: usar Redis efemero via Testcontainers para validar `RedisCacheManager`, JSON, TTLs e round-trip dos objetos cacheados.
- Desde o commit 1, contextos Spring de teste que nao validam Redis diretamente devem usar Redis efemero Testcontainers ou uma substituicao de bean de teste equivalente, para `./gradlew build` nao depender de Redis externo.
- `docker-compose.yml` contem `redis` e app roda multi-instancia contra Redis.
- `REFERENCE_POINTS.md` contem `v3.4-cache-distribuido`.
- Tag imutavel `v3.4-cache-distribuido` criada no SHA revisado e ja enviado ao PR apos validacao, sem merge automatico em `main`.
- PR final revisado e corrigido.

## Fora de escopo

- AWS ElastiCache.
- Write-through ou write-behind.
- Mitigacao de stampede ou penetration.
- Cache multinivel.
- Qualquer mudanca no vault ObsidianPos.
