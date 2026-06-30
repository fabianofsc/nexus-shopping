# Nexus Shopping Backend

Backend REST API built with Kotlin, Gradle, Java 21, Spring Boot 4, Actuator, Flyway, PostgreSQL, and JPA/JDBC.

The project is intentionally shaped as a performance demonstration: it creates a large relational product catalog, documents the baseline without secondary indexes, adds targeted read indexes, and then adds pagination to compare the impact on high-traffic catalog queries.

## Didactic Scenarios

Three branches represent the evolution of the system, each published as a Docker Hub image:

| Branch | Image | What changes |
| --- | --- | --- |
| `missing-index-performance-baseline` | `fabianofsc/nexus-shopping:baseline` | No secondary indexes |
| `add-product-query-indexes` | `fabianofsc/nexus-shopping:indexes` | Adds indexes on `category_id` and `name` |
| `add-products-pagination` | `fabianofsc/nexus-shopping:pagination` | Limits results to 50 per request |
| `main` | `fabianofsc/nexus-shopping:latest` | Always the latest version |

## Architecture

The codebase follows hexagonal architecture (Ports and Adapters), applied incrementally. Business logic lives in the `application` layer and is isolated from HTTP and JDBC details via port interfaces.

```
product/
  domain/          → pure business types (Product, ProductPage)
  application/
    port/outbound/ → ProductRepositoryPort (interface)
    usecase/       → ProductSearchUseCase, ProductCreateUseCase, typed exceptions
  adapter/
    inbound/http/  → ProductController, request DTOs
    outbound/jdbc/ → ProductRepository (JdbcTemplate)
```

Key constraints:
- Domain and use-case layers have no Spring or JDBC imports.
- `ProductValidationException` is thrown by use cases; the controller catches only this type so server errors are never masked as HTTP 400.
- Validation lives in the use case so it is reusable by any future adapter (CLI, queue, batch).

## Load Test Quick Start

To run the comparative load tests, you only need **Docker** and **JMeter**. No Java or Gradle required.

```bash
git clone https://github.com/fabianofsc/nexus-shopping.git
cd nexus-shopping

# Step 1: start the scenario (pulls image from Docker Hub)
make start-baseline    # resets DB, seeds 10M products, waits for health

# Step 2: run JMeter tests
make jmeter-all SCENARIO=baseline

# Step 3: move to next scenario (reuses the existing database)
make start-indexes
make jmeter-all SCENARIO=indexes

make start-pagination
make jmeter-all SCENARIO=pagination
```

The database is created only once (at baseline). Each subsequent scenario applies only the new Flyway migrations on top of the existing data.

See `docs/jmeter-test-guide.md` for the full guide, including installation instructions, expected results, and how to interpret JMeter reports.

## Requirements

For load testing only:

- Docker and Docker Compose
- Apache JMeter

For local development:

- Java 21
- Docker and Docker Compose
- Gradle Wrapper, already included as `./gradlew`
- Apache JMeter, only for load testing and report generation

Install JMeter on macOS:

```bash
brew install jmeter
```

## Database

PostgreSQL runs through Docker Compose:

```bash
docker compose up -d postgres
```

Default database settings:

- URL: `jdbc:postgresql://localhost:5432/nexus_shopping`
- Database: `nexus_shopping`
- User: `nexus`
- Password: `nexus`

Flyway runs automatically on application startup. The migrations create:

- `brands`
- `categories`
- `products`
- `10,000,000` seeded products by default
- `1,000` seeded brands by default
- `500` seeded categories by default
- `idx_products_category_id` for category reads
- `idx_products_name` for name prefix reads

The seed size is configurable:

```bash
PRODUCT_SEED_COUNT=100000 ./gradlew bootRun
```

If migrations were already applied and then changed during local experimentation, reset the local database volume:

```bash
docker compose down -v
docker compose up -d postgres
```

## Run

```bash
docker compose up -d postgres
./gradlew bootRun
```

Health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Search by category:

```bash
curl -o /dev/null -w '%{time_total}s %{size_download} bytes\n' \
  'http://localhost:8080/products?categoryId=1&page=0&size=50'
```

Search by product name:

```bash
curl -o /dev/null -w '%{time_total}s %{size_download} bytes\n' \
  'http://localhost:8080/products?name=Product%202999999&page=0&size=50'
```

Create a product:

```bash
curl -X POST http://localhost:8080/products \
  -H 'Content-Type: application/json' \
  -d '{"brandId":1,"categoryId":1,"sku":"SKU-001","name":"New Product","slug":"new-product","priceAmount":49.90}'
```

The product repository executes these read queries:

```sql
SELECT * FROM products WHERE category_id = ? ORDER BY id LIMIT ? OFFSET ?
SELECT * FROM products WHERE name >= ? AND name < ? AND name LIKE ? ORDER BY name LIMIT ? OFFSET ?
```

The endpoint returns a slice response with `content`, `page`, `size`, `count`, and `hasNext`. It intentionally avoids `COUNT(*)` so each request performs only the page read. The name lookup still uses `LIKE`, but it is written as a prefix range so the portable B-tree index on `products.name` can be used.

## Docker Image

The project uses Spring Boot's native Gradle task for OCI image generation through Cloud Native Buildpacks. There is no Dockerfile.

Build the local image:

```bash
./gradlew bootBuildImage --imageName nexus-shopping:local
```

Run PostgreSQL and the app with Docker Compose:

```bash
APP_IMAGE=nexus-shopping:local docker compose up -d postgres app
```

To build and push the scenario images to Docker Hub:

```bash
make push-baseline
make push-indexes
make push-pagination
make push-all
```

To switch to one of the didactic branches, build the image locally, and run the full stack:

```bash
scripts/run-stack.sh baseline --reset-db
scripts/run-stack.sh indexes --reset-db
scripts/run-stack.sh pagination --reset-db
```

## Test

```bash
./gradlew build
```

The automated tests validate:

- Spring Boot starts with the Actuator health endpoint.
- Flyway runs automatically.
- The product endpoint works with a small test seed.
- The migrations stay portable between PostgreSQL and H2.
- The read indexes are explicitly present and no `UNIQUE` constraints are introduced.

## Lint

Kotlin linting is configured with ktlint through Gradle. In the first adoption phase, lint is opt-in and does not run as part of `build`.

Check Kotlin and Gradle Kotlin DSL style:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintCheck
```

Autoformat Kotlin and Gradle Kotlin DSL files locally:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintFormat
```

Run `ktlintFormat` only when the resulting diff is reviewed as a mechanical formatting change.

## Load Test with JMeter

JMeter test plans are versioned at:

```
load-tests/jmeter/products-by-category.jmx
load-tests/jmeter/products-by-name.jmx
```

The recommended workflow uses Docker Hub images and the Makefile targets:

```bash
make start-baseline       # pull image, reset DB, seed 10M products, wait health
make jmeter-all SCENARIO=baseline

make start-indexes        # swap image, Flyway applies indexes, wait health
make jmeter-all SCENARIO=indexes

make start-pagination     # swap image, no new migrations, wait health
make jmeter-all SCENARIO=pagination
```

Or run everything in one command per scenario:

```bash
make load-hub-baseline
make load-hub-indexes
make load-hub-pagination
```

HTML reports are saved to `build/jmeter-report/`. Committed reference reports for all three scenarios are available under `docs/jmeter-reports/`.

The full load test guide with installation instructions, expected results, and report interpretation is at:

```
docs/jmeter-test-guide.md
```

The documented result summaries are at:

```
docs/load-test-results-20260626.md          baseline without indexes
docs/load-test-index-results-20260626.md    with indexes
docs/load-test-pagination-results-20260627.md  with pagination
```
