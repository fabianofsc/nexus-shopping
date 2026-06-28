# AGENTS.md

## Project Snapshot

- Backend REST API for a product catalog performance lab.
- Stack: Kotlin, Java 21, Gradle Wrapper, Spring Boot 4, Actuator, Flyway, PostgreSQL, JdbcTemplate (no JPA/ORM by choice).
- The project is intentionally didactic: it compares missing indexes, read indexes, and pagination under JMeter load, and also demonstrates hexagonal architecture incrementally.
- Current main feature branch: `hexagonal-architecture`.
- Docker Hub repo: `fabianofsc/nexus-shopping` with tags `baseline`, `indexes`, `pagination`, `latest`.

## Architecture

The project follows hexagonal architecture (Ports and Adapters). Dependency direction is always inward: adapters → application → domain.

```
product/
  domain/                              # pure business types, no framework imports
    Product.kt
    ProductPage.kt
  application/
    port/outbound/
      ProductRepositoryPort.kt         # outbound port (interface)
    usecase/
      ProductSearchUseCase.kt          # orchestration + validation
      ProductCreateUseCase.kt          # orchestration + validation
      CreateProductCommand.kt          # use-case input
      ProductValidationException.kt    # typed exception; controller catches only this
  adapter/
    inbound/http/
      ProductController.kt             # translates HTTP → use case → HTTP
      CreateProductRequest.kt          # HTTP DTO with toCommand()
    outbound/jdbc/
      ProductRepository.kt             # implements ProductRepositoryPort via JdbcTemplate
```

Key design decisions:
- JPA/ORM rejected to preserve domain purity and didactic JDBC value.
- `ProductValidationException` is thrown by use cases; the controller catches only this type so real server errors are never masked as HTTP 400.
- `SimpleJdbcInsert` is used for INSERT to avoid KeyHolder verbosity.
- Validation lives in the use case, not in the controller, so any future adapter (CLI, queue) reuses the same rules.

## Local Command Rules

- Always prefix shell commands with `rtk`.
- Use the Gradle wrapper, not a system Gradle:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

- Use Docker Compose through `rtk`:

```bash
rtk docker compose up -d postgres
rtk docker compose down -v
rtk docker compose ps
```

- Use `rg`/`rg --files` for search.
- Do not commit build outputs or generated JMeter HTML reports under `build/`.

## Git Branches

- `missing-index-performance-baseline`: preserves the version without secondary read indexes.
- `add-product-query-indexes`: adds product read indexes and an indexable prefix lookup.
- `add-products-pagination`: branch with paginated product search.
- `hexagonal-architecture`: accumulation branch for all architectural changes; merges into `main` when complete.
  - `feat/hexagonal-refactor` → `hexagonal-architecture` (PR #1): package reorganization, port interface, use cases, ProductValidationException.
  - `feat/product-create` → `hexagonal-architecture` (PR #2 planned): POST /products create flow.
- `main`: latest stable version.
- Branch names must be in English.
- Keep commits grouped by context: code, migrations, tests, load-test docs, generated report assets.

## Application Behavior

- Health endpoint:

```http
GET /actuator/health
```

- Product search endpoint:

```http
GET /products?categoryId=1&page=0&size=50
GET /products?name=Product%209999999&page=0&size=50
```

- The endpoint accepts either `categoryId` or `name`, never both.
- Pagination parameters:
  - `page` default: `0`
  - `size` default: `50`
  - valid `size`: `1..500`
- Response is a slice, not a full page:

```json
{
  "content": [],
  "page": 0,
  "size": 50,
  "count": 50,
  "hasNext": true
}
```

- Do not add `COUNT(*)` to the request path unless the task explicitly asks for total counts.
- The slice strategy reads `size + 1` rows to calculate `hasNext`.

- Product create endpoint (implemented in `feat/product-create` PR):

```http
POST /products
Content-Type: application/json

{
  "brandId": 1,
  "categoryId": 1,
  "sku": "SKU-001",
  "name": "Product Name",
  "slug": "product-name",
  "priceAmount": 49.90
}
```

- Optional fields with defaults: `description` (null), `status` ("ACTIVE"), `currency` ("BRL"), `inventoryQuantity` (0).
- Returns `201 Created` with the persisted product including the generated `id`.
- Validation errors return `400 Bad Request`.

## Database And Migrations

- Migrations live in `src/main/resources/db/migration`.
- Current schema has three core tables:
  - `brands`
  - `categories`
  - `products`
- Default seed size is controlled by `PRODUCT_SEED_COUNT`, defaulting to `10000000`.
- Keep migrations portable between PostgreSQL and H2 unless the user explicitly changes that rule.
- Avoid PostgreSQL-only types/functions in migrations used by tests.
- Current indexes:

```sql
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_name ON products (name);
```

- The name search intentionally uses prefix range bounds so the B-tree index can be used:

```sql
SELECT *
FROM products
WHERE name >= ?
  AND name < ?
  AND name LIKE ?
ORDER BY name
LIMIT ?
OFFSET ?
```

## Tests

- Main verification command:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

- Tests use H2 and small Flyway seed placeholders.
- Keep tests validating:
  - Actuator health includes DB status.
  - Flyway migrations run.
  - catalog seed remains relationally valid.
  - migrations stay portable.
  - product search returns the paginated slice contract.

## Docker Hub

- Images are published automatically by GitHub Actions on push to each branch.
- To start a scenario without building locally:

```bash
rtk make start-baseline     # resets DB, pulls baseline image, waits health
rtk make start-indexes      # swaps image, keeps DB, waits health
rtk make start-pagination   # swaps image, keeps DB, waits health
```

- To reset the DB explicitly for any scenario:

```bash
rtk make hub-reset-baseline
rtk make hub-reset-indexes
rtk make hub-reset-pagination
```

- To build and push images manually:

```bash
rtk make push-baseline
rtk make push-indexes
rtk make push-pagination
```

## JMeter

- JMeter is external tooling, not a Spring dependency.
- Always invoke JMeter through the wrapper to suppress startup warnings:

```bash
scripts/jmeter.sh
```

- Plans live in:
  - `load-tests/jmeter/products-by-category.jmx`
  - `load-tests/jmeter/products-by-name.jmx`
- Standard profile: 50 threads, 20s ramp-up, 120s duration.
- Use Makefile targets instead of invoking JMeter directly:

```bash
rtk make jmeter-all SCENARIO=baseline
rtk make jmeter-category SCENARIO=indexes
rtk make jmeter-name SCENARIO=pagination
```

- If JMeter reports `java.net.SocketException: Operation not permitted`, rerun with elevated permission because local high-concurrency HTTP may be sandbox-blocked.

## Documentation

- Keep load-test result summaries in `docs/`.
- Current result docs:
  - `docs/load-test-results-20260626.md`: missing-index baseline.
  - `docs/load-test-index-results-20260626.md`: indexed query comparison.
  - `docs/load-test-pagination-results-20260627.md`: pagination comparison.
- Store committed chart PNGs under `docs/assets/load-tests/`.
- Committed JMeter HTML reports live under `docs/jmeter-reports/baseline/`, `indexes/`, `pagination/`.
- Keep docs concise and in Portuguese for result explanations.
- Prefer ASCII in new or edited files unless there is a strong reason not to.

## Performance Lessons Preserved By This Repo

- Missing indexes make selective reads collapse on large relational tables.
- Indexes reduce the cost of locating rows.
- Pagination reduces the cost of returning rows.
- A B-tree index on `name` is useful only when the query shape allows it, such as prefix/range lookup.
- Avoid returning huge product lists from API endpoints intended for repeated catalog reads.
