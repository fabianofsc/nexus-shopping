# Nexus Shopping Backend

Backend REST API built with Kotlin, Gradle, Java 21, Spring Boot 4, Actuator, Flyway, PostgreSQL, and JPA/JDBC.

The project is intentionally shaped as a performance demonstration: it creates a large relational product catalog, documents the baseline without secondary indexes, then adds targeted read indexes to compare the impact on high-traffic catalog queries.

## Requirements

- Java 21
- Docker and Docker Compose
- Gradle Wrapper, already included as `./gradlew`
- Apache JMeter, only for load testing and report generation

Install JMeter on macOS:

```bash
brew install jmeter
```

Or download it from https://jmeter.apache.org/download_jmeter.cgi.

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

Override with:

```bash
DB_URL=jdbc:postgresql://localhost:5432/nexus_shopping
DB_USERNAME=nexus
DB_PASSWORD=nexus
```

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

Product endpoint:

```bash
curl -o /dev/null -w '%{time_total}s %{size_download} bytes\n' \
  'http://localhost:8080/products?categoryId=1'
```

Search by product name:

```bash
curl -o /dev/null -w '%{time_total}s %{size_download} bytes\n' \
  'http://localhost:8080/products?name=Product%202999999'
```

The product repository executes these read queries:

```sql
SELECT * FROM products WHERE category_id = ?
SELECT * FROM products WHERE name >= ? AND name < ? AND name LIKE ?
```

The name lookup still uses `LIKE`, but it is written as a prefix range so the portable B-tree index on `products.name` can be used. The branch `missing-index-performance-baseline` preserves the version without secondary indexes.

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

The app service uses the internal Compose hostname `postgres`:

```bash
DB_URL=jdbc:postgresql://postgres:5432/nexus_shopping
```

To switch to one of the didactic branches, build the image, and run the full stack:

```bash
scripts/run-stack.sh baseline --reset-db
scripts/run-stack.sh indexes --reset-db
scripts/run-stack.sh pagination --reset-db
```

The script maps:

- `baseline` to `missing-index-performance-baseline`
- `indexes` to `add-product-query-indexes`
- `pagination` to `add-products-pagination`
- `main` to `main`

The local `latest` image should always be built from `main`, which represents the latest paginated version:

```bash
make image-latest
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

## Load Test with JMeter

The JMeter test plans are versioned at:

```bash
load-tests/jmeter/products-by-category.jmx
load-tests/jmeter/products-by-name.jmx
```

Run the app first:

```bash
docker compose down -v
docker compose up -d postgres
./gradlew bootRun
```

Run the load test:

```bash
mkdir -p build/jmeter-results build/jmeter-report
jmeter -n \
  -t load-tests/jmeter/products-by-category.jmx \
  -l build/jmeter-results/products-by-category.jtl \
  -e -o build/jmeter-report/products-by-category \
  -Jthreads=10 \
  -JrampUp=10 \
  -Jduration=60 \
  -Jhost=localhost \
  -Jport=8080 \
  -JcategoryId=1
```

Run the name search load test:

```bash
mkdir -p build/jmeter-results build/jmeter-report
jmeter -n \
  -t load-tests/jmeter/products-by-name.jmx \
  -l build/jmeter-results/products-by-name.jtl \
  -e -o build/jmeter-report/products-by-name \
  -Jthreads=5 \
  -JrampUp=10 \
  -Jduration=60 \
  -Jhost=localhost \
  -Jport=8080 \
  -Jname='Product 2999999'
```

Open the HTML report:

```bash
open build/jmeter-report/products-by-category/index.html
open build/jmeter-report/products-by-name/index.html
```

JMeter is not an application dependency. It is an external tool used to run load tests and generate demonstrative HTML reports.

The full JMeter workflow guide is available at:

```bash
docs/jmeter-test-guide.md
```

The documented baseline run without indexes is available at:

```bash
docs/load-test-results-20260626.md
```

The documented comparison run with indexes is available at:

```bash
docs/load-test-index-results-20260626.md
```
