# Reference Points for Testing & Learning

Este documento lista as branches e tags imutáveis que servem como pontos de referência para testes comparativos e estudos educacionais.

---

## 📌 Tags de Referência

### v1.0-baseline
**Baseline: Sem Indexes Secundários**

- **Branch:** `missing-index-performance-baseline`
- **Commit:** (veja `git show v1.0-baseline`)
- **Propósito:** Estado do código SEM otimizações de index para servir como baseline
- **Testado em:** 2026-06-30
- **Stack:** Kotlin, Java 21, Spring Boot 4.1, PostgreSQL, H2 (testes)
- **Documentação:** [docs/load-test-results-baseline.md](docs/load-test-results-baseline.md)
- **Como clonar:**
  ```bash
  git clone --branch v1.0-baseline https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Propósito de aprendizado:** Ver como queries sem index afetam performance em tabelas grandes

---

### v1.1-indexes
**Otimização: Indexes em category_id e name**

- **Branch:** `add-product-query-indexes`
- **Commit:** (veja `git show v1.1-indexes`)
- **Propósito:** Primeira otimização: adicionar indexes secundários
- **Testado em:** 2026-06-30
- **Melhorias em relação a v1.0:**
  - Index B-tree em `products(category_id)` — acelera buscas por categoria
  - Index B-tree em `products(name)` — acelera buscas por nome (prefix range lookup)
- **Documentação:** [docs/load-test-results-indexes.md](docs/load-test-results-indexes.md)
- **Como clonar:**
  ```bash
  git clone --branch v1.1-indexes https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Propósito de aprendizado:** Ver impacto de indexes em queries seletivas

---

### v1.2-pagination
**Otimização: Paginação sem COUNT(*)**

- **Branch:** `add-products-pagination`
- **Commit:** (veja `git show v1.2-pagination`)
- **Propósito:** Segunda otimização: implementar paginação Slice
- **Testado em:** 2026-06-30
- **Melhorias em relação a v1.1:**
  - Uso de `Slice<T>` em vez de `Page<T>` — elimina `COUNT(*)`
  - Query lê `size + 1` linhas para calcular `hasNext` sem contar
  - Endpoint retorna `{content, page, size, count, hasNext}` sem overhead de contagem
- **Documentação:** [docs/load-test-results-pagination.md](docs/load-test-results-pagination.md)
- **Como clonar:**
  ```bash
  git clone --branch v1.2-pagination https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Propósito de aprendizado:** Ver como eliminar queries custosas (COUNT) mantendo paginação funcional

---

### v2.0-hexagonal
**Arquitetura: Hexagonal (Ports & Adapters)**

- **Branch:** `hexagonal-architecture`
- **Commit:** (veja `git show v2.0-hexagonal`)
- **Propósito:** Refatoração completa para arquitetura hexagonal
- **Merged em:** 2026-06-30
- **Mudanças arquiteturais:**
  - Estrutura: `domain/` → `application/` → `adapter/`
  - Dependency inversion: adapter → application → domain
  - Zero imports de framework em `domain/` e `application/`
  - DTOs conversão: `DTO.toCommand()` → `Command.toEntity()` → `Entity.toDomain()`
  - Validação centralizada em use cases
- **Documentação:** [docs/superpowers/specs/2026-06-28-hexagonal-refactor-design.md](docs/superpowers/specs/2026-06-28-hexagonal-refactor-design.md)
- **Como clonar:**
  ```bash
  git clone --branch v2.0-hexagonal https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Propósito de aprendizado:** Entender Ports & Adapters, inversão de dependência, isolamento de domínio

---

### v3.0-scalability
**Escalabilidade: NGINX Load Balancer distribuindo 3 instâncias**

- **Branch:** `scalability-and-load-balancer`
- **Commit:** (veja `git show v3.0-scalability`)
- **Propósito:** Demonstrar escala horizontal com reverse proxy NGINX distribuindo tráfego entre 3 instâncias da aplicação
- **Merged em:** 2026-07-05
- **Mudanças:**
  - Reverse proxy NGINX (`nginx/nginx.conf`) fazendo load balancing entre 3 réplicas da aplicação
  - Endpoint `GET /instance-info` retornando hostname e timestamp — permite ver qual instância atendeu cada request
  - Seed de dados local via `.env` para a branch
- **Documentação:** [docs/scalability-and-load-balancer/load-balancing-nginx.md](docs/scalability-and-load-balancer/load-balancing-nginx.md), [docs/scalability-and-load-balancer/load-balancing-test-plan.md](docs/scalability-and-load-balancer/load-balancing-test-plan.md)
- **Como clonar:**
  ```bash
  git clone --branch v3.0-scalability https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Propósito de aprendizado:** Ver load balancing auto-hospedado (NGINX) distribuindo tráfego entre múltiplas instâncias na prática

---

### v3.1-load-balancer-cloud
**Escalabilidade na nuvem: Load Balancer gerenciado (AWS ALB + Auto Scaling Group)**

- **Commit:** mesmo de `v3.0-scalability` (veja `git show v3.1-load-balancer-cloud`)
- **Propósito:** Marcar o estado do código usado na demonstração do Load Balancer gerenciado na AWS (Application Load Balancer + Auto Scaling Group). A evolução é de **infraestrutura** (AWS), não de código de aplicação — por isso a tag aponta para o mesmo commit de `v3.0`.
- **Merged em:** 2026-07-09 (tag criada a partir da `main`)
- **Imagem Docker Hub:** `:load-balancer-cloud` não publicada (mesma limitação de CI do v3.0)
- **Como clonar:**
  ```bash
  git clone --branch v3.1-load-balancer-cloud https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Propósito de aprendizado:** Ver load balancing gerenciado na nuvem (ALB) e elasticidade automática (ASG) — contraste com o NGINX auto-hospedado de v3.0

---

### v3.2-product-detail
**Endpoint de detalhe de produto (leitura por chave única) + carga JMeter para cache**

- **Branch:** `codex/get-product-by-id` (PR #14)
- **Commit:** (veja `git show v3.2-product-detail`)
- **Propósito:** Preparar a fixture para a aula de Cache — adicionar uma leitura por registro único (`GET /products/{id}`), o caso limpo de cache-aside (hot key), e o plano de carga para medir p95 antes/depois do cache
- **Merged em:** 2026-07-16
- **Mudanças:**
  - `GET /products/{id}` (`getById`) no `ProductController`, com `findById` no `ProductRepositoryPort` e no adapter JPA, `ProductGetByIdUseCase` e 404 via `ProductNotFoundException`
  - `load-tests/jmeter/product-by-id.jmx` com a knob `hotSet` (`id = __Random(1, hotSet)`): `hotSet` pequeno = alta repetição (cache brilha); grande = acesso uniforme (cache inútil) — demonstra ganho **e** limite do cache
- **Imagem Docker Hub:** não publicada
- **Como clonar:**
  ```bash
  git clone --branch v3.2-product-detail https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Propósito de aprendizado:** Base para cache-aside — leitura quente e repetida por chave única

---

### v3.3-cache-aside
**Cache-aside LOCAL (Caffeine) no detalhe de produto**

- **Branch:** `add-product-cache-aside` (PR #17)
- **Commit:** (veja `git show v3.3-cache-aside`)
- **Propósito:** Caso limpo de cache-aside LOCAL (in-process) sobre `GET /products/{id}`, implementado à mão como decorator explícito da arquitetura hexagonal — base da aula de Cache. A incoerência entre instâncias é **intencional** (demonstrada na aula rodando este código com múltiplas instâncias); não é resolvida aqui.
- **Base:** `v3.2-product-detail`
- **Mudanças:**
  - Dependência `com.github.ben-manes.caffeine:caffeine` (gerenciada pelo BOM do Spring Boot); sem `spring-boot-starter-cache`/`@EnableCaching`
  - `CachingProductRepositoryAdapter` (`@Primary`) decora o `ProductJpaRepositoryAdapter` e implementa `ProductRepositoryPort`:
    - `findById`: cache-aside explícito com as 4 operações visíveis (`getIfPresent` → miss → `delegate.findById` → `put`), log **HIT/MISS**, nunca cacheia `null`
    - `updatePrice`: delega ao JPA e **invalida localmente** (`cache.invalidate(id)`) — invalidação LOCAL apenas
    - `findByCategoryId` / `findByName` / `save`: pass-through direto (não cacheados)
  - Config `nexus.cache.product.max-size` (10000) e `.ttl` (10m) via `application.yml` (`ProductCacheProperties` + `@Configuration` construindo `Cache<Long, Product>`)
  - Domínio e use cases permanecem cache-unaware; contrato dos endpoints inalterado
- **Guardrails (fora de escopo, reservado à v3.4):** sem `@Cacheable`/`@CacheEvict`/Spring Cache abstraction, sem Redis/spring-data-redis, sem cache da busca paginada
- **Imagem Docker Hub:** não publicada para a tag `v3.3-cache-aside` em si. **Atenção:** esta seção descreve o estado ORIGINAL de 3 instâncias (PR #17) — a tag `v3.3-cache-aside` foi recriada em 2026-07-20 apontando para uma topologia reduzida a 1 instância (ver nota histórica abaixo). O commit original de 3 instâncias permanece acessível pela branch `add-product-cache-aside`, que **tem** imagem publicada: `fabianofsc/nexus-shopping:add-product-cache-aside` (usada no Bloco 2 do roteiro de Cache Distribuído, para reproduzir o bug de incoerência entre instâncias).
- **Como clonar:**
  ```bash
  git clone --branch v3.3-cache-aside https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Propósito de aprendizado:** Ver cache-aside explícito reduzir o custo de leituras quentes repetidas por chave, e observar (na aula) o limite da abordagem LOCAL — incoerência entre instâncias

> **Nota histórica — `v3.3-cache-aside` foi recriada em 2026-07-20:** a tag original (PR #17, branch `add-product-cache-aside`) tinha a topologia de 3 instâncias descrita acima. Decisão do professor: mover a tag para apontar para o commit com a topologia reduzida (1 instância + Load Balancer, sem Redis) — mesmo tratamento de `v3.2.1-single-instance` — permitindo comparação "antes"/"depois" limpa no Bloco 1 de Cache. O código de cache-aside em si não mudou, só a infraestrutura. Branch atual da tag: `lb-single-instance-cache-aside` (protegida). Espelhado em [[Projeto Nexus Shopping]] no vault.

---

### v3.4-cache-distribuido
**Cache distribuido Redis com Spring Cache no detalhe e nas buscas paginadas**

- **Branch:** `codex/v3.4-cache-distribuido` (branch remota removida apos o merge; tag recriada em 2026-07-22 sobre o commit atualizado — mesmo tratamento historico de `v3.3-cache-aside`)
- **Commit:** (veja `git show v3.4-cache-distribuido`)
- **Proposito:** Evoluir o cache-aside local para Redis compartilhado entre instancias, mantendo o dominio e os use cases sem conhecimento de cache.
- **Base:** `v3.3-cache-aside`
- **Mudancas principais:**
  - Redis como unica store de cache, com chaves String e valores JSON legiveis usando `GenericJackson2JsonRedisSerializer`.
  - Spring Cache no adapter JPA: detalhe em `products:detail` e buscas paginadas em `products:search`.
  - TTL de 10 minutos para detalhe e 30 segundos para buscas; `save` e `updatePrice` invalidam todas as buscas cacheadas.
  - Chaves de busca incluem categoria ou nome, pagina e tamanho para evitar colisao entre slices.
  - `docker-compose.yml`: `APP_IMAGE` padrao aponta para `fabianofsc/nexus-shopping:v3.4-cache-distribuido` — `docker compose up -d` (sem `--build`) ja baixa a imagem publicada.
  - `docker-compose.yml`: servico `redisinsight` (`redis/redisinsight:3.8.0`, nativo arm64/amd64) exposto em `:5540` para inspecao visual do cache, alternativa ao `redis-cli`.
- **Imagem Docker Hub:** `fabianofsc/nexus-shopping:v3.4-cache-distribuido`
- **Como clonar:**
  ```bash
  git clone --branch v3.4-cache-distribuido https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Verificacao manual com multiplas instancias:**
  ```bash
  docker compose up -d --scale app1=1 --scale app2=1 --scale app3=1
  curl http://localhost:8080/products/1
  curl http://localhost:8080/products?name=Product%201&page=0&size=3
  ```
  Repita as requisicoes pelo NGINX, faca um `PATCH` em uma instancia e confirme que a proxima leitura em outra instancia reflete o valor atualizado porque a store de cache e compartilhada.
- **Inspecao do Redis:**
  ```bash
  docker compose exec redis redis-cli --raw keys '*products*'
  docker compose exec redis redis-cli --raw get '<key>'
  ```
  O valor retornado deve ser JSON e incluir campos como `name` e `priceAmount`.
- **Proposito de aprendizado:** Comparar cache local e distribuido, observar serializacao JSON, TTL por tipo de leitura e a invalidacao de paginas apos escrita.

---

## 📊 Relação entre versões

```
v1.0-baseline (sem otimizações)
    ↓
v1.1-indexes (+ indexes)
    ↓
v1.2-pagination (+ paginação sem COUNT)
    ↓ (refatoração completa)
v2.0-hexagonal (nova arquitetura)
    ↓
v3.0-scalability (NGINX load balancer, 3 instâncias)
    ↓
v3.1-load-balancer-cloud (LB gerenciado AWS: ALB + Auto Scaling — mesmo commit de v3.0)
    ↓
v3.2-product-detail (GET /products/{id} + carga JMeter para cache)
    ↓
v3.3-cache-aside (cache-aside local com Caffeine)
    ↓
v3.4-cache-distribuido (Redis compartilhado + Spring Cache + busca cacheada)
```

**Comparar performance:**
```bash
# Clonar v1.0 e rodar JMeter
git clone --branch v1.0-baseline ...
make start-baseline
scripts/jmeter.sh products-by-name

# Depois, v1.1
git clone --branch v1.1-indexes ...
make start-indexes
scripts/jmeter.sh products-by-name

# Comparar resultados em docs/load-test-results-*.md
```

---

## 🎓 Para Alunos

Use estas tags como pontos de estudo:

1. **Estudar v1.0:** Entender a base do projeto e performance sem otimizações
2. **Comparar v1.0 vs v1.1:** Ver impacto de indexes em SQL
3. **Comparar v1.1 vs v1.2:** Entender eliminar queries custosas
4. **Estudar v2.0:** Aprender arquitetura hexagonal na prática
5. **Estudar v3.0:** Ver load balancing com NGINX distribuindo tráfego entre instâncias

Cada tag tem:
- ✅ Código compilável e testável
- ✅ Documentação de design no `docs/superpowers/specs/`
- ✅ Resultados de testes em `docs/load-test-results-*.md`
- ✅ Histórico de mudanças em commits

---

## 🔍 Verificar uma tag

```bash
# Ver informações da tag
git show v1.1-indexes

# Ver commit de uma tag
git rev-list -n 1 v1.1-indexes

# Verificar checksum (para reprodutibilidade)
git rev-parse v1.1-indexes
```

---

**Última atualização:** 2026-07-17
**Responsible:** Fabiano Góes
**Tags ativas:** v1.0-baseline, v1.1-indexes, v1.2-pagination, v2.0-hexagonal, v3.0-scalability, v3.1-load-balancer-cloud, v3.2-product-detail, v3.3-cache-aside, v3.4-cache-distribuido
