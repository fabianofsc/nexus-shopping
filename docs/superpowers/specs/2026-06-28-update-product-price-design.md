# Update Product Price — Design Spec

**Date:** 2026-06-28
**Status:** approved

## Goal

Add a `PATCH /products/{id}` endpoint that updates only the `priceAmount` of an existing product and returns the full updated product.

## HTTP Contract

```
PATCH /products/{id}
Content-Type: application/json

{ "priceAmount": 99.90 }
```

| Scenario | Status | Body |
|---|---|---|
| Success | 200 OK | full `Product` |
| `priceAmount <= 0` | 400 Bad Request | validation message |
| `id` not found | 404 Not Found | error message |

## Architecture

Follows the existing hexagonal structure. Dependency direction: adapter → application → domain.

### New files

```
product/
  application/
    usecase/
      UpdatePriceCommand.kt          # data class: id: Long, priceAmount: BigDecimal
      UpdateProductPriceUseCase.kt   # validates + calls port + handles not-found
      ProductNotFoundException.kt    # typed exception for 404
  adapter/
    inbound/http/
      UpdatePriceRequest.kt          # HTTP DTO: priceAmount: BigDecimal, toCommand(id)
```

### Modified files

| File | Change |
|---|---|
| `ProductRepositoryPort.kt` | + `updatePrice(id: Long, priceAmount: BigDecimal): Product?` |
| `ProductRepository.kt` | + implementation via `UPDATE … RETURNING *` |
| `ProductController.kt` | + `PATCH /products/{id}` mapped to use case |

## Data Flow

```
ProductController.updatePrice(id, UpdatePriceRequest)
  → UpdateProductPriceUseCase.execute(UpdatePriceCommand(id, priceAmount))
      → priceAmount <= 0 → throw ProductValidationException ("priceAmount must be greater than zero")
      → productRepository.updatePrice(id, priceAmount): Product?
          → null → throw ProductNotFoundException ("Product $id not found")
      → return Product
  → 200 OK with full Product body
```

## SQL

Single roundtrip — no separate SELECT:

```sql
UPDATE products
SET price_amount = ?, updated_at = CURRENT_TIMESTAMP
WHERE id = ?
RETURNING *
```

## Validation Rules

- `priceAmount` must be strictly greater than zero (`> 0`)
- `priceAmount == 0` is rejected (same as negative)

## Error Handling

- `ProductValidationException` → controller maps to 400 Bad Request (existing pattern)
- `ProductNotFoundException` → controller maps to 404 Not Found (new catch block, same style)

## Tests

**New: `UpdateProductPriceUseCaseTest`** (unit, port mocked)
- happy path: valid price + product exists → returns updated `Product`
- `priceAmount < 0` → `ProductValidationException`
- `priceAmount == 0` → `ProductValidationException` (boundary)
- product not found → `ProductNotFoundException`

**Existing: `CatalogMigrationContractTest`**
- No new migration needed; current schema already supports the operation.

**Extended: `ProductControllerTest`** (SpringBootTest + random port + raw HttpClient — same pattern as PR #2)
- `PATCH /products/{id}` with valid price → 200 with updated product
- `PATCH /products/{id}` with `priceAmount = 0` → 400
- `PATCH /products/{id}` with non-existent id → 404

> PR #2 introduced `ProductControllerTest` with this pattern; PATCH tests follow the same conventions.

## Out of Scope

- Updating any field other than `priceAmount`
- Bulk price updates
- Price history / audit log
- Currency change
