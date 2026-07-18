# Cache Distribuido Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Evoluir a fixture `v3.3-cache-aside` para `v3.4-cache-distribuido`, com Redis distribuido, Spring Cache e cache curto para busca paginada.

**Architecture:** Manter arquitetura hexagonal: dominio e use cases seguem cache-unaware, e o cache fica no adapter outbound JPA. A evolucao acontece em tres commits didaticos: Redis explicito, Spring Cache no detalhe, Spring Cache nas buscas.

**Tech Stack:** Kotlin 2.2, Java 21, Spring Boot 4.1, Spring Data JPA, Spring Data Redis, Spring Cache, Redis, PostgreSQL, H2 em testes.

## Global Constraints

- Ponto de partida: tag `v3.3-cache-aside`.
- Estado final/tag: `v3.4-cache-distribuido`.
- Redis e a unica store de cache no estado final; sem Caffeine e sem cache multinivel.
- Commit 1 deve manter cache-aside explicito no `CachingProductRepositoryAdapter`, mas usando Redis em vez de Caffeine.
- Commit 2 deve remover o decorator explicito e usar Spring Cache com `@Cacheable` em `findById` e `@CacheEvict` em `updatePrice`.
- Commit 3 deve cachear `findByCategoryId` e `findByName` em cache separado com TTL curto, exemplo `30s`.
- Serializer Redis deve ser `GenericJackson2JsonRedisSerializer`; nunca serializer JDK. Chaves Redis devem usar `StringRedisSerializer`.
- TTL por cache: detalhe de produto mais longo; busca curta; sem TTL infinito.
- Chaves de busca devem conter todos os parametros: `categoryId/name`, `page`, `size`.
- Cache de `null` deve ser desabilitado e/ou evitado com `unless = "#result == null"`.
- `save` e `updatePrice` devem invalidar o cache de busca (`allEntries = true`) para evitar paginas obsoletas depois de escrita.
- Testes automatizados que validam Redis real devem usar Testcontainers Redis (`redis:7-alpine`) com `@DynamicPropertySource`, sem depender de Redis externo ja ligado.
- Desde o commit 1, contextos Spring de teste que nao validam Redis diretamente devem usar Redis efemero Testcontainers ou uma substituicao de bean de teste equivalente, para `./gradlew build` nao depender de Redis externo.
- Dominio/use cases permanecem cache-unaware.
- Contrato dos endpoints inalterado.
- Load-tests intocados.
- `domain/` e `application/` sem imports de `jakarta.persistence`, `org.hibernate` ou `org.springframework.data`.
- Leituras JPA com `@Query` JPQL explicito; sem derived queries.
- Paginacao via `Slice` sem `COUNT(*)`.
- Usar `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build`.
- Ao final, abrir PR, executar loop de revisao/correcao, fazer push de toda correcao revisada, e criar tag imutavel `v3.4-cache-distribuido` no SHA revisado e ja enviado ao PR, sem merge automatico em `main`.

---

## File Structure

- `build.gradle.kts`: trocar/remover dependencias de cache conforme cada commit.
- `docker-compose.yml`: adicionar Redis e ligar apps ao Redis.
- `src/main/resources/application.yml`: propriedades de Redis e TTLs de cache.
- `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/CachingProductRepositoryAdapter.kt`: commit 1 usa Redis explicito; commit 2 remove o arquivo.
- `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductCacheConfig.kt`: commit 1 configura `RedisTemplate`; commit 2 configura Spring Cache/RedisCacheManager.
- `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductCacheProperties.kt`: representar TTLs necessarios (`ttl` para detalhe, `searchTtl` para busca).
- `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductJpaRepositoryAdapter.kt`: commit 2/3 adiciona anotacoes Spring Cache nos metodos do adapter.
- `src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/*`: substituir testes de Caffeine/decorator por testes focados em Redis explicito e Spring Cache.
- `REFERENCE_POINTS.md`: registrar a tag `v3.4-cache-distribuido`.
- `.superpowers/sdd/progress.md`: registrar progresso do fluxo SDD.
- `src/test/resources/application.yml` or `src/test/resources/application-test.yml`: configurar isolamento de Redis/cache para testes desde o commit 1.

### Task 1: Redis Explicito

**Files:**
- Modify: `build.gradle.kts`
- Modify: `docker-compose.yml`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/CachingProductRepositoryAdapter.kt`
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductCacheConfig.kt`
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductCacheProperties.kt`
- Modify: `src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/CachingProductRepositoryAdapterTest.kt`
- Modify: `src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/CachingProductRepositoryWiringTest.kt`
- Create or modify: `src/test/resources/application.yml`
- Create: `src/test/kotlin/com/nexus/shopping/support/RedisTestConfiguration.kt`

**Interfaces:**
- Produces: explicit Redis cache-aside decorator with constructor `(delegate: ProductJpaRepositoryAdapter, redisTemplate: RedisTemplate<String, Product>, properties: ProductCacheProperties)`.
- Produces: cache key format `products:detail::<id>` for detail cache.
- Produces: config values `nexus.cache.product.ttl` and Redis host/port wiring.
- Produces: `RedisTemplate<String, Product>` with `StringRedisSerializer` keys and `GenericJackson2JsonRedisSerializer` values.
- Produces: test-time Redis isolation for all `@SpringBootTest` contexts before the first Redis-backed build.

- [ ] **Step 1: Write failing tests for explicit Redis cache-aside**

Replace the Caffeine-specific unit test with tests using a fake or mocked `RedisTemplate<String, Product>` and value operations:

```kotlin
@Test
fun `findById returns cached product from Redis without hitting delegate`() {
    val delegate = FakeProductRepositoryAdapter()
    val redisTemplate = mockRedisTemplate()
    val adapter = CachingProductRepositoryAdapter(delegate, redisTemplate, ProductCacheProperties(ttl = Duration.ofMinutes(10)))

    every { valueOperations.get("products:detail::1") } returns existingProduct(id = 1L)

    val result = adapter.findById(1L)

    assertEquals(existingProduct(id = 1L), result)
    assertEquals(0, delegate.findByIdCalls)
}
```

Also cover miss populates Redis with TTL, `null` is not cached, `updatePrice` deletes `products:detail::<id>`, and the `RedisTemplate` bean uses String keys plus JSON value serializer.

Add Testcontainers dependency in Task 1 because the explicit Redis decorator can be reached by existing `@SpringBootTest` contexts before Spring Cache exists:

```kotlin
testImplementation("org.testcontainers:junit-jupiter")
```

Create a shared test configuration that starts `redis:7-alpine` once and publishes `spring.data.redis.host`, `spring.data.redis.port`, and `management.health.redis.enabled=true` through `@DynamicPropertySource` or Spring Boot test service connection support available in this project. If a global dynamic property hook is awkward in Kotlin tests, use an abstract base class or imported test configuration and update every existing `@SpringBootTest` class to inherit/import it in Task 1.

- [ ] **Step 2: Run focused test and verify RED**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "*CachingProductRepositoryAdapterTest"`

Expected: fail because Caffeine constructor/config still exist.

- [ ] **Step 3: Implement Redis explicit cache-aside**

Use `RedisTemplate<String, Product>` with `opsForValue().get(key)`, `opsForValue().set(key, product, properties.ttl)`, and `delete(key)`. Configure `keySerializer` and `hashKeySerializer` with `StringRedisSerializer`; configure `valueSerializer` and `hashValueSerializer` with `GenericJackson2JsonRedisSerializer(objectMapper)`. Keep HIT/MISS logs at INFO. Keep `findByCategoryId`, `findByName`, `save` pass-through.

- [ ] **Step 4: Wire Redis dependencies and docker-compose**

Add `implementation("org.springframework.boot:spring-boot-starter-data-redis")`; remove `com.github.ben-manes.caffeine:caffeine`. Add a `redis` service using `redis:7-alpine`, healthcheck `redis-cli ping`, port `6379:6379`, and make app services depend on Redis. Add `SPRING_DATA_REDIS_HOST=redis` and `SPRING_DATA_REDIS_PORT=6379` to app environment. Add equivalent defaults in `application.yml`.

- [ ] **Step 5: Run focused tests and full build**

Run:
```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "*CachingProductRepositoryAdapterTest" --tests "*CachingProductRepositoryWiringTest"
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

- [ ] **Step 6: Commit**

```bash
git add build.gradle.kts docker-compose.yml src/main/resources/application.yml src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/CachingProductRepositoryAdapter.kt src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductCacheConfig.kt src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductCacheProperties.kt src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/CachingProductRepositoryAdapterTest.kt src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/CachingProductRepositoryWiringTest.kt src/test/kotlin/com/nexus/shopping/support/RedisTestConfiguration.kt src/test/resources/application.yml
git commit -m "feat: move explicit product cache to redis"
```

### Task 2: Spring Cache no Detalhe

**Files:**
- Modify: `build.gradle.kts`
- Delete: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/CachingProductRepositoryAdapter.kt`
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductCacheConfig.kt`
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductCacheProperties.kt`
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductJpaRepositoryAdapter.kt`
- Delete or replace: `src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/CachingProductRepositoryAdapterTest.kt`
- Modify: `src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/CachingProductRepositoryWiringTest.kt`
- Create: `src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductSpringCacheTest.kt`
- Create: `src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductRedisCacheIntegrationTest.kt`
- Create or modify: `src/test/resources/application.yml`

**Interfaces:**
- Consumes: Redis dependency and product TTL from Task 1.
- Produces: cache names `products:detail` and `products:search`.
- Produces: Spring Cache annotations on the actual outbound adapter.
- Produces: JSON Redis serialization via `GenericJackson2JsonRedisSerializer`.
- Produces: no null caching for product detail.
- Produces: `save` invalidates all entries from `products:search`.

- [ ] **Step 1: Write failing Spring Cache tests for detail**

Create a Spring test that boots with cache enabled, spies or mocks `SpringDataProductRepository`, calls `ProductRepositoryPort.findById(1L)` twice, and verifies the underlying repository is hit once. Then call `updatePrice(1L, BigDecimal("88.80"))` and verify the next `findById(1L)` returns the updated price and hits the repository again. Also verify missing ids are not cached.

Use the test cache profile for this semantic test: cache in memory and Redis health disabled. Keep the production Redis cache configuration covered by `ProductRedisCacheIntegrationTest`.

- [ ] **Step 2: Run focused test and verify RED**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "*ProductSpringCacheTest"`

Expected: fail because the decorator still owns cache behavior and no Spring Cache annotations exist.

- [ ] **Step 3: Add Spring Cache configuration**

Add `implementation("org.springframework.boot:spring-boot-starter-cache")`. Add `testImplementation("org.testcontainers:junit-jupiter")`. Add `@EnableCaching`. Define a `RedisCacheManager` using `RedisCacheConfiguration.defaultCacheConfig().serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())).serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer(objectMapper))).disableCachingNullValues()`. Ensure the `ObjectMapper` registers Kotlin and Java time support via Spring's configured mapper. Configure TTL for `products:detail` with `properties.ttl` and a default finite TTL.

Make the production Redis cache manager conditional so tests can opt into an in-memory manager:
- production/default: Redis cache manager;
- tests/default: `spring.cache.type=simple` or `nexus.cache.redis.enabled=false`, plus `management.health.redis.enabled=false`;
- Redis integration test: overrides properties to use production Redis cache manager and Testcontainers host/port.

Create `ProductRedisCacheIntegrationTest` using `GenericContainer<Nothing>("redis:7-alpine")` with exposed port `6379` and `@DynamicPropertySource` to set `spring.data.redis.host`, `spring.data.redis.port`, and the property that enables the production Redis cache manager. This test must use production `RedisCacheManager`, not a mock or in-memory cache.

- [ ] **Step 4: Move cache behavior to `ProductJpaRepositoryAdapter`**

Delete `CachingProductRepositoryAdapter`. Annotate:

```kotlin
@Cacheable(cacheNames = [ProductCacheConfig.PRODUCT_DETAIL_CACHE], key = "#id", unless = "#result == null")
override fun findById(id: Long): Product? = repository.findById(id).orElse(null)?.toDomain()

@CacheEvict(cacheNames = [ProductCacheConfig.PRODUCT_DETAIL_CACHE], key = "#id")
override fun updatePrice(id: Long, priceAmount: BigDecimal): Product? { ... }
```

Use `@Caching` on `updatePrice` if needed so it also evicts `products:search` with `allEntries = true`. Annotate `save` to evict `products:search` with `allEntries = true`.

- [ ] **Step 5: Update wiring tests**

Replace the decorator assertion with an assertion that `ProductRepositoryPort` resolves to a Spring proxy around `ProductJpaRepositoryAdapter` and that the decorator class no longer exists in code.

- [ ] **Step 6: Run focused tests and full build**

Run:
```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "*ProductSpringCacheTest" --tests "*CachingProductRepositoryWiringTest"
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "*ProductRedisCacheIntegrationTest"
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

- [ ] **Step 7: Commit**

```bash
git add build.gradle.kts src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa
git commit -m "feat: use spring cache for product detail"
```

### Task 3: Busca Cacheada e Documentacao

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductCacheConfig.kt`
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductCacheProperties.kt`
- Modify: `src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductJpaRepositoryAdapter.kt`
- Modify: `src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductSpringCacheTest.kt`
- Modify: `REFERENCE_POINTS.md`

**Interfaces:**
- Consumes: `ProductCacheConfig.PRODUCT_DETAIL_CACHE` from Task 2.
- Produces: `ProductCacheConfig.PRODUCT_SEARCH_CACHE`.
- Produces: `nexus.cache.product.search-ttl: 30s`.

- [ ] **Step 1: Write failing tests for search cache**

Extend `ProductSpringCacheTest` to verify:
- two identical `findByCategoryId(1L, 0, 2)` calls hit `SpringDataProductRepository.findByCategoryId` once;
- `findByCategoryId(1L, 0, 2)` and `findByCategoryId(1L, 1, 2)` are separate cache entries;
- two identical `findByName("Product 1", 0, 3)` calls hit `SpringDataProductRepository.findByNamePrefix` once;
- different `name`, `page`, or `size` values do not collide.
- `updatePrice` evicts `products:search`, so a subsequent identical search reflects the new price instead of stale cached data.
- `save` evicts `products:search`, so a subsequent identical search can include newly persisted matching data.

Extend `ProductRedisCacheIntegrationTest` to verify:
- Redis keys are strings;
- cached values are readable JSON containing product fields such as `"name"` and `"priceAmount"`;
- `PTTL` for `products:detail` is greater than the short search TTL;
- `PTTL` for `products:search` is finite and close to `30s`.

- [ ] **Step 2: Run focused test and verify RED**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "*ProductSpringCacheTest"`

Expected: fail because search methods are not cached yet.

- [ ] **Step 3: Configure short TTL and annotations**

Add `searchTtl: Duration = Duration.ofSeconds(30)` to `ProductCacheProperties`, add `search-ttl: 30s` to `application.yml`, configure `products:search` in `RedisCacheManager`, and annotate:

```kotlin
@Cacheable(cacheNames = [ProductCacheConfig.PRODUCT_SEARCH_CACHE], key = "'category:' + #categoryId + ':page:' + #page + ':size:' + #size")
override fun findByCategoryId(...)

@Cacheable(cacheNames = [ProductCacheConfig.PRODUCT_SEARCH_CACHE], key = "'name:' + #name + ':page:' + #page + ':size:' + #size")
override fun findByName(...)
```

If using Spring's default key prefixing, account for the physical Redis key prefix in tests with key scans rather than hard-coded full physical key strings.

- [ ] **Step 4: Document distributed verification and JSON values**

Update `REFERENCE_POINTS.md` entry for `v3.4-cache-distribuido` with:
- proposito;
- base `v3.3-cache-aside`;
- mudancas principais;
- como clonar;
- comando de verificacao manual para multi-instancia (`docker compose up --scale app1=1 --scale app2=1 --scale app3=1` or existing compose services);
- exemplo `redis-cli --raw keys '*products*'` and `redis-cli --raw get '<key>'` showing values JSON;
- rodape `Tags ativas` including `v3.4-cache-distribuido`;
- `Ultima atualizacao: 2026-07-17`.

- [ ] **Step 5: Run verification**

Run:
```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "*ProductSpringCacheTest"
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "*ProductRedisCacheIntegrationTest"
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
rg -n "Caffeine|CachingProductRepositoryAdapter|com.github.ben-manes" build.gradle.kts src
```

Expected: tests/build pass; `rg` returns no matches for removed Caffeine/decorator.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/application.yml src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductCacheConfig.kt src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductCacheProperties.kt src/main/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductJpaRepositoryAdapter.kt src/test/kotlin/com/nexus/shopping/product/adapter/outbound/jpa/ProductSpringCacheTest.kt REFERENCE_POINTS.md docs/superpowers/specs/2026-07-17-cache-distribuido-design.md docs/superpowers/plans/2026-07-17-cache-distribuido.md
git commit -m "feat: cache product search with short ttl"
```

### Task 4: PR, Revisao Final e Tag

**Files:**
- Modify if review requires fixes: files named by final reviewer
- No planned source changes unless review finds issues

**Interfaces:**
- Consumes: three logical feature commits from Tasks 1-3.
- Produces: pushed branch, reviewed PR, immutable tag `v3.4-cache-distribuido` on the reviewed PR SHA.

- [ ] **Step 1: Run final verification before PR**

Run:
```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
rg -n "Caffeine|CachingProductRepositoryAdapter|com.github.ben-manes" build.gradle.kts src
git status --short
```

Expected: build passes, no removed cache/decorator vestiges, and working tree clean except possibly ignored `.superpowers/sdd/` scratch files.

- [ ] **Step 2: Push branch and open PR**

Create `.superpowers/sdd/pr-body.md` with summary, tests run, distributed verification steps, JSON Redis inspection command, and the line `🤖 Generated with Codex`.

Run:
```bash
git push -u origin codex/v3.4-cache-distribuido
gh pr create --base main --head codex/v3.4-cache-distribuido --title "feat: add distributed product cache" --body-file .superpowers/sdd/pr-body.md
```

- [ ] **Step 3: Final review loop**

Dispatch a final reviewer subagent with the whole branch diff. For every Critical or Important finding:
- implement fixes;
- rerun covering tests;
- respond in the report with what changed;
- commit and `git push` the fix branch so the PR head contains the reviewed fix;
- re-dispatch review.

Repeat until final reviewer reports no Critical/Important findings. Inline review comments generated by agents must end with `🤖 Generated with Claude Code` or `🤖 Generated with Codex`.

- [ ] **Step 4: Create and push immutable tag**

After PR is reviewed and corrected, without merging `main` automatically, verify the local HEAD equals the pushed PR head:

```bash
git fetch origin codex/v3.4-cache-distribuido
test "$(git rev-parse HEAD)" = "$(git rev-parse origin/codex/v3.4-cache-distribuido)"
```

Then create and push the immutable tag:

```bash
git tag v3.4-cache-distribuido
git push origin v3.4-cache-distribuido
git rev-parse v3.4-cache-distribuido
```

- [ ] **Step 5: Final report**

Report branch, PR URL, tag commit SHA, commits created, tests run, and summary for vault mirroring.
