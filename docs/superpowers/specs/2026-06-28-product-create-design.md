# PR #2: POST /products — Create Product Flow

**Status:** Merged (2026-06-28)
**Branch:** `feat/product-create` → `hexagonal-architecture`
**PR:** https://github.com/fabianofsc/nexus-shopping/pull/2

---

## Problem

After PR #1, the product module had a clean hexagonal structure for the search flow, but no write path. `ProductRepositoryPort` only declared read operations. There was no way to create products via the API.

Additionally, the V2 seed migration was inserting explicit `id` values for `brands`, `categories`, and `products`, which caused the database IDENTITY sequences to fall out of sync — any subsequent `INSERT` without an explicit `id` would collide with seeded rows.

---

## Goal

Implement the full `POST /products` vertical slice following the hexagonal architecture established in PR #1:

1. Accept a JSON payload and return the created product with `201 Created`.
2. Validate all fields in the use case (no validation in the controller).
3. Persist using JdbcTemplate (no ORM) and return the generated `id`.
4. Fix the V2 migration so IDENTITY sequences advance naturally.

---

## Architecture

The feature follows the same layer boundaries from PR #1. A new vertical slice is added in parallel to the search flow, not modifying its components.

```
product/
├── application/
│   ├── port/outbound/
│   │   └── ProductRepositoryPort.kt     ← + save(command): Product
│   └── usecase/
│       ├── CreateProductCommand.kt      ← use-case input (no Spring/Jackson imports)
│       └── ProductCreateUseCase.kt      ← all validation + delegates to port
└── adapter/
    ├── inbound/http/
    │   ├── CreateProductRequest.kt      ← HTTP DTO with toCommand()
    │   └── ProductController.kt        ← + @PostMapping, returns 201
    └── outbound/jdbc/
        └── ProductRepository.kt        ← + save() via SimpleJdbcInsert
```

---

## Components

### `CreateProductCommand`

Use-case input object. Pure data class; no Spring or Jackson imports. Defaults applied here (`status = "ACTIVE"`, `currency = "BRL"`, `inventoryQuantity = 0`) rather than in the HTTP layer, so any future adapter inherits the same defaults.

```kotlin
data class CreateProductCommand(
    val brandId: Long,
    val categoryId: Long,
    val sku: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val status: String = "ACTIVE",
    val priceAmount: BigDecimal,
    val currency: String = "BRL",
    val inventoryQuantity: Int = 0,
)
```

### `ProductCreateUseCase`

Validates all 13 rules and delegates to `ProductRepositoryPort.save()`. Throws `ProductValidationException` for any violation — same pattern as `ProductSearchUseCase`.

Validation rules:
| Field | Rule |
|-------|------|
| `brandId` | > 0 |
| `categoryId` | > 0 |
| `sku` | not blank; max 120 chars |
| `name` | not blank; max 220 chars |
| `slug` | not blank; max 260 chars |
| `description` | max 2000 chars (nullable) |
| `status` | one of: `ACTIVE`, `INACTIVE`, `ARCHIVED` |
| `priceAmount` | >= 0 |
| `currency` | exactly 3 chars, not blank |
| `inventoryQuantity` | >= 0 |

### `CreateProductRequest`

HTTP DTO deserialized from JSON. Maps optional fields with HTTP-layer defaults and exposes `toCommand()` to convert to `CreateProductCommand`. Spring/Jackson annotations live here, not in the command.

Optional fields with defaults:
- `description` → `null`
- `status` → `"ACTIVE"`
- `currency` → `"BRL"`
- `inventoryQuantity` → `0`

### `ProductRepositoryPort.save()`

New method added to the outbound port:

```kotlin
fun save(command: CreateProductCommand): Product
```

### `ProductRepository.save()`

Implemented via `SimpleJdbcInsert` (avoids `KeyHolder` verbosity). Returns the generated `id`, then re-fetches the full row with `SELECT * FROM products WHERE id = ?` to return a consistent `Product` object.

### `ProductController` — `@PostMapping`

```kotlin
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
fun create(@RequestBody request: CreateProductRequest): Product {
    try {
        return productCreateUseCase.create(request.toCommand())
    } catch (e: ProductValidationException) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
    }
}
```

Returns `201 Created` with the persisted product body. Catches only `ProductValidationException` → HTTP 400.

---

## API contract

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

**Response 201 Created:**
```json
{
  "id": 10000001,
  "brandId": 1,
  "categoryId": 1,
  "sku": "SKU-001",
  "name": "Product Name",
  "slug": "product-name",
  "description": null,
  "status": "ACTIVE",
  "priceAmount": 49.90,
  "currency": "BRL",
  "inventoryQuantity": 0
}
```

**Response 400 Bad Request** — on any validation failure, message describes the violated rule.

---

## Migration fix (V2)

Removed explicit `id` columns from all `INSERT INTO brands`, `INSERT INTO categories`, and `INSERT INTO products` statements so the IDENTITY sequence advances naturally. Without this fix, the sequence would remain at 0 and any subsequent application-driven `INSERT` would collide with seeded rows.

---

## Test strategy

- **`ProductCreateUseCaseTest`** — 13 unit tests covering all validation rules; no Spring context, no database. Uses a fake port.
- **`ProductControllerTest`** — integration tests with `@SpringBootTest` + H2 in-memory database:
  - `201 Created` with correct response body
  - `400 Bad Request` for blank `sku`, negative `priceAmount`, invalid `status`
- All existing tests (`ProductSearchUseCaseTest`, `CatalogMigrationContractTest`, `HealthEndpointTest`) pass unchanged.
- `./gradlew test` — BUILD SUCCESSFUL.

---

## Key decisions

| Decision | Rationale |
|----------|-----------|
| Defaults in `CreateProductCommand`, not in `CreateProductRequest` | Any future adapter (CLI, batch) inherits the same defaults without duplicating them |
| `SimpleJdbcInsert` over `KeyHolder` | Less verbose; cleaner generated-key extraction |
| Re-fetch after insert | Returns consistent `Product` with all DB-computed fields (timestamps, defaults) |
| Validation in use case only | Controller stays a thin adapter; rules reusable by any future adapter |
| `ProductValidationException` only caught in controller | Server errors (DB failure, etc.) propagate as 5xx, not masked as 400 |
