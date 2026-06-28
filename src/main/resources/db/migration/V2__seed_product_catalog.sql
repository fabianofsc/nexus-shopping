INSERT INTO brands (id, name, description)
WITH digits(n) AS (
    SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
),
numbers(n) AS (
    SELECT ones.n + tens.n * 10 + hundreds.n * 100 + 1
    FROM digits ones
    CROSS JOIN digits tens
    CROSS JOIN digits hundreds
)
SELECT
    n,
    'Brand ' || CAST(n AS VARCHAR),
    'Illustrative catalog brand ' || CAST(n AS VARCHAR)
FROM numbers
WHERE n <= 1000;

INSERT INTO categories (id, parent_id, name, slug, status)
WITH digits(n) AS (
    SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
),
numbers(n) AS (
    SELECT ones.n + tens.n * 10 + 1
    FROM digits ones
    CROSS JOIN digits tens
)
SELECT
    n,
    NULL,
    'Category ' || CAST(n AS VARCHAR),
    'category-' || CAST(n AS VARCHAR),
    'ACTIVE'
FROM numbers
WHERE n <= 50;

INSERT INTO categories (id, parent_id, name, slug, status)
WITH digits(n) AS (
    SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
),
numbers(n) AS (
    SELECT ones.n + tens.n * 10 + hundreds.n * 100 + 51
    FROM digits ones
    CROSS JOIN digits tens
    CROSS JOIN digits hundreds
)
SELECT
    n,
    MOD(n - 51, 50) + 1,
    'Category ' || CAST(n AS VARCHAR),
    'category-' || CAST(n AS VARCHAR),
    'ACTIVE'
FROM numbers
WHERE n <= 500;

INSERT INTO products (
    id,
    brand_id,
    category_id,
    sku,
    name,
    slug,
    description,
    status,
    price_amount,
    currency,
    inventory_quantity
)
WITH digits(n) AS (
    SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
),
numbers(n) AS (
    SELECT
        units.n
        + tens.n * 10
        + hundreds.n * 100
        + thousands.n * 1000
        + ten_thousands.n * 10000
        + hundred_thousands.n * 100000
        + millions.n * 1000000
        + 1
    FROM digits units
    CROSS JOIN digits tens
    CROSS JOIN digits hundreds
    CROSS JOIN digits thousands
    CROSS JOIN digits ten_thousands
    CROSS JOIN digits hundred_thousands
    CROSS JOIN digits millions
)
SELECT
    n,
    MOD(n - 1, 1000) + 1,
    MOD(n - 1, 500) + 1,
    'SKU-' || CAST(n AS VARCHAR),
    'Product ' || CAST(n AS VARCHAR),
    'product-' || CAST(n AS VARCHAR),
    'Illustrative product ' || CAST(n AS VARCHAR) || ' used to demonstrate read performance without indexes.',
    CASE
        WHEN MOD(n, 25) = 0 THEN 'ARCHIVED'
        WHEN MOD(n, 10) = 0 THEN 'INACTIVE'
        ELSE 'ACTIVE'
    END,
    CAST((1000 + MOD(n, 90000)) / 100.0 AS NUMERIC(12, 2)),
    'BRL',
    MOD(n, 500)
FROM numbers
WHERE n <= ${productSeedCount};
