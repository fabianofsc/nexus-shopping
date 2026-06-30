# AGENTS.md

## Comunicacao

- Responda sempre em portugues, independente do idioma da mensagem recebida.
- Planejamento, analise e documentacao devem ser escritos em portugues.

## Project Snapshot

- Backend REST API educacional: catalogo de produtos com evolucao incremental de performance e arquitetura.
- Stack: Kotlin, Java 21, Gradle Wrapper, Spring Boot 4, Actuator, Flyway, PostgreSQL, Spring Data JPA.
- Docker Hub: `fabianofsc/nexus-shopping` com tags `baseline`, `indexes`, `pagination`, `latest`.

## Architecture

Arquitetura hexagonal (Ports and Adapters). Direcao de dependencia: `adapter -> application -> domain`.

```
<domain>/
  domain/           # tipos puros; zero imports de framework
  application/
    port/outbound/  # interfaces outbound ({Domain}RepositoryPort)
    usecase/        # orquestracao, validacao, commands, excecoes tipadas
  adapter/
    inbound/http/   # controllers + DTOs; DTO tem toCommand()
    outbound/jpa/   # {Domain}Entity, Spring Data repository, {Domain}JpaRepositoryAdapter
```

Convencoes de conversao (invariante em todos os dominios):
- DTO HTTP -> command: `toCommand()` no DTO
- Command -> entity JPA: `toEntity()` extension em `{Domain}Entity.kt`
- Entity JPA -> domain: `toDomain()` em `{Domain}Entity`

Decisoes fixas:
- `domain/` e `application/` sem imports de `jakarta.persistence`, `org.hibernate` ou `org.springframework.data`.
- Validacao vive no use case; adapter nao valida.
- Leituras JPA com `@Query` JPQL explicito; sem derived queries.
- Paginacao via `Slice` sem `COUNT(*)`.

## Local Command Rules

- Usar o Gradle wrapper, nao Gradle do sistema:

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

- Docker Compose: `docker compose up -d postgres` / `docker compose down -v`
- Busca de arquivos/texto com `rg` / `rg --files`.
- Nao commitar outputs de build nem relatorios HTML do JMeter em `build/`.

## Git Workflow

- Branch names em ingles.
- Commits agrupados por contexto: code, migrations, tests, docs, assets.
- Nunca fazer merge de branch sem confirmacao explicita do usuario para aquele merge especifico.
- Toda execucao de spec/plan deve acontecer em uma **git worktree isolada**, nunca diretamente na branch de acumulacao.
- Ao final de toda execucao de spec/plan, perguntar ao usuario: merge local, abrir PR, manter branch ou descartar.
- Ao final de toda atualizacao do CLAUDE.md, verificar se o total de linhas fica abaixo de 200; mover conteudo para `docs/agents/` com @import se necessario.

Branches atuais:
- `missing-index-performance-baseline`: sem indexes secundarios.
- `add-product-query-indexes`: indexes em `category_id` e `name`.
- `add-products-pagination`: busca paginada.
- `hexagonal-architecture`: branch de acumulacao para mudancas arquiteturais; merges em `main` quando completo.
- `main`: versao estavel mais recente.

@docs/agents/api-endpoints.md

## Database And Migrations

- Migrations: `src/main/resources/db/migration`.
- Tabelas: `brands`, `categories`, `products`. Seed: `PRODUCT_SEED_COUNT` (default `10000000`).
- Manter migrations portaveis entre PostgreSQL e H2 salvo instrucao explicita do usuario.
- Evitar tipos/funcoes exclusivos do PostgreSQL em migrations usadas por testes.
- Indexes atuais:

```sql
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_name ON products (name);
```

- Busca por nome usa prefix range bounds para usar o B-tree index:

```sql
SELECT * FROM products WHERE name >= ? AND name < ? AND name LIKE ? ORDER BY name LIMIT ? OFFSET ?
```

## Tests

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

- Tests usam H2 e seeds Flyway reduzidos.
- Validar: health do Actuator, Flyway, seed relacional valido, migrations portaveis, contrato de slice.

@docs/agents/load-testing.md

## Documentation

- Load-test results: `docs/load-test-results-*.md`; charts: `docs/assets/load-tests/`.
- JMeter HTML reports: `docs/jmeter-reports/baseline/`, `indexes/`, `pagination/`.
- Decisions: `docs/decisions/YYYY-MM-DD-prd-<topic>.md`.
- Specs: `docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md` (sem `prd` no nome de arquivo).
- Plans: `docs/superpowers/plans/YYYY-MM-DD-<topic>.md` (nunca incluir `prd` no nome).
- Docs em portugues. Preferir ASCII em arquivos novos ou editados.

## Performance Lessons

- Indexes faltantes colapsam leituras seletivas em tabelas relacionais grandes.
- Indexes reduzem o custo de localizar linhas; paginacao reduz o custo de retornar linhas.
- Index B-tree em `name` so e util quando o shape da query permite uso (prefix/range lookup).
- Evitar retornar listas enormes em endpoints de catalogo de alta frequencia de leitura.
