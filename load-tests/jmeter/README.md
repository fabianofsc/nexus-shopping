# JMeter Load Test

This project keeps the JMeter test plan in source control, but JMeter itself is an external load testing tool. It is not a normal application library and does not need to be added to the Spring Boot classpath.

## Install

macOS:

```bash
brew install jmeter
```

Or download Apache JMeter from https://jmeter.apache.org/download_jmeter.cgi.

## Prepare the App

Start PostgreSQL and the application:

```bash
docker compose down -v
docker compose up -d postgres
./gradlew bootRun
```

By default the Flyway seed creates 10,000,000 products, 1,000 brands, and 500 categories. Each category receives about 20,000 products. The current endpoint is paginated with `page` and `size`.

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

## Endpoints Under Test

```http
GET /products?categoryId=1&page=0&size=50
GET /products?name=Product%202999999&page=0&size=50
```

The repository intentionally executes paginated reads:

```sql
SELECT * FROM products WHERE category_id = ? ORDER BY id LIMIT ? OFFSET ?
SELECT * FROM products WHERE name >= ? AND name < ? AND name LIKE ? ORDER BY name LIMIT ? OFFSET ?
```

The category test is useful for demonstrating why indexes alone are not enough when an endpoint returns a very large payload. Pagination limits the number of rows materialized and serialized per request.
