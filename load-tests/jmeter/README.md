# JMeter Load Test

This project keeps the JMeter test plan in source control, but JMeter itself is an external load testing tool. It is not a normal application library and does not need to be added to the Spring Boot classpath.

## Install

macOS:

```bash
brew install jmeter
```

Or download Apache JMeter from https://jmeter.apache.org/download_jmeter.cgi.

## Prepare the App

Start the full stack with Docker Compose — no local JDK/Gradle required, students only need Docker:

```bash
docker compose down -v
docker compose up --build -d
docker compose ps
```

Wait until all services report healthy, then hit the app through nginx at `localhost:8080` (the `app1`/`app2`/`app3` containers only `expose` port 8080 to the Docker network — nginx is the only container with a published port on the host).

By default `.env` sets `PRODUCT_SEED_COUNT=1000` for a fast boot. That is fine for functional checks, but too small to see a meaningful cache hit-ratio contrast (with only 1,000 products, a `hotSet` anywhere close to 1,000 already covers the whole catalog). For load tests — especially the "large hot set / near-uniform access" run described below — seed a larger catalog:

```bash
docker compose down -v
PRODUCT_SEED_COUNT=10000000 docker compose up --build -d
```

This seeds 10,000,000 products, 1,000 brands, and 500 categories (about 20,000 products per category). The current endpoint is paginated with `page` and `size`. Product ids are generated from 1 through the configured product seed count — keep `hotSet`/`-JhotSet` within that range.

## Run the Category Test

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
  -JcategoryId=1 \
  -Jpage=0 \
  -Jsize=50
```

Open the HTML report:

```bash
open build/jmeter-report/products-by-category/index.html
```

## Run the Name Search Test

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
  -Jname='Product 2999999' \
  -Jpage=0 \
  -Jsize=50
```

Open the HTML report:

```bash
open build/jmeter-report/products-by-name/index.html
```

## Run the Product Detail Test

```bash
mkdir -p build/jmeter-results build/jmeter-report
jmeter -n \
  -t load-tests/jmeter/product-by-id.jmx \
  -l build/jmeter-results/product-by-id.jtl \
  -e -o build/jmeter-report/product-by-id \
  -Jthreads=10 \
  -JrampUp=10 \
  -Jduration=60 \
  -Jhost=localhost \
  -Jport=8080 \
  -JhotSet=1000
```

Open the HTML report:

```bash
open build/jmeter-report/product-by-id/index.html
```

`hotSet` controls how many product ids are randomly selected during the test. A small hot set, such as `1000`, creates repeated hot-key reads where cache-aside should help; a large hot set, up to the total product count, approaches uniform access where cache has much less impact.

## Endpoints Under Test

```http
GET /products?categoryId=1&page=0&size=50
GET /products?name=Product%202999999&page=0&size=50
GET /products/{id}
```

The repository intentionally executes paginated reads:

```sql
SELECT * FROM products WHERE category_id = ? ORDER BY id LIMIT ? OFFSET ?
SELECT * FROM products WHERE name >= ? AND name < ? AND name LIKE ? ORDER BY name LIMIT ? OFFSET ?
```

The category test is useful for demonstrating why indexes alone are not enough when an endpoint returns a very large payload. Pagination limits the number of rows materialized and serialized per request.
