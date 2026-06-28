# PR #1: Hexagonal Architecture Refactor — Product Module

**Status:** Merged (2026-06-28)
**Branch:** `feat/hexagonal-refactor` → `hexagonal-architecture`
**PR:** https://github.com/fabianofsc/nexus-shopping/pull/1

---

## Problem

The product module had a flat package structure where all concerns lived at the same level (`com.nexus.shopping.product`):

- `ProductController` — HTTP handler AND validation logic
- `ProductService` — thin delegation with no clear boundary
- `ProductRepository` — persistence

Validation rules were scattered in the controller (HTTP layer), making them unavailable to any future adapter. The dependency between `ProductService` and `ProductRepository` was direct — no interface, no inversion. The architecture was invisible from the package layout.

---

## Goal

Apply the Ports and Adapters pattern (Hexagonal Architecture) to the product module so that:

1. Business logic lives in a framework-free application layer.
2. Adapters (HTTP, JDBC) depend on the application — never the reverse.
3. The architecture is visible from the directory structure alone.
4. Existing tests pass without modification.

---

## Architecture

### Package layout after this PR

```
product/
├── domain/
│   ├── Product.kt
│   └── ProductPage.kt
├── application/
│   ├── port/outbound/
│   │   └── ProductRepositoryPort.kt       ← outbound port (interface)
│   └── usecase/
│       ├── ProductSearchUseCase.kt        ← validation + search routing
│       └── ProductValidationException.kt  ← typed domain exception
└── adapter/
    ├── inbound/http/
    │   └── ProductController.kt           ← thin HTTP adapter
    └── outbound/jdbc/
        └── ProductRepository.kt           ← implements ProductRepositoryPort
```

### Dependency rule

```
adapter → application → domain
```

No layer imports from the layer above it. Domain has zero framework imports.

---

## Components

### `ProductRepositoryPort` (outbound port)

Interface that defines what the application needs from persistence. The use case depends on this abstraction, not on `ProductRepository` directly.

```kotlin
interface ProductRepositoryPort {
    fun findByCategoryId(categoryId: Long, page: Int, size: Int): ProductPage
    fun findByName(name: String, page: Int, size: Int): ProductPage
}
```

### `ProductSearchUseCase` (renamed from `ProductService`)

Owns all validation rules for the search operation. Throws `ProductValidationException` (not `ResponseStatusException`) so no Spring imports leak into the application layer.

Validation rules moved here from `ProductController`:
- `categoryId` and `name` are mutually exclusive
- `page >= 0`
- `size` in `1..500`
- At least one of `categoryId` or `name` must be provided

### `ProductValidationException`

Typed exception thrown by use cases. The controller catches only this type, ensuring real server errors (e.g., database failures) are never masked as HTTP 400.

### `ProductController`

Reduced to a thin HTTP adapter: deserializes the request, calls the use case, and translates `ProductValidationException` into HTTP 400. No business logic.

### `ProductRepository`

Unchanged implementation; moved to `adapter/outbound/jdbc/` and now implements `ProductRepositoryPort`.

---

## Test strategy

- **`ProductSearchUseCaseTest`** — 6 unit tests with no Spring context and no database. Port implemented as an in-memory tracking fake (anonymous object) that records which method was called, making routing assertions explicit.
- Existing `HealthEndpointTest` (3 tests) and `CatalogMigrationContractTest` (6 tests) pass unchanged.
- Total: 15 tests, 0 failures.

---

## Key decisions

| Decision | Rationale |
|----------|-----------|
| JPA/ORM rejected | Preserves domain purity; keeps JDBC value didactic |
| `ProductValidationException` (typed) | Prevents use-case catch blocks from masking real server errors as HTTP 400 |
| Validation in use case, not controller | Any future adapter (CLI, queue, batch) reuses the same rules |
| Package structure mirrors architecture | Architecture visible from directory layout without reading code |
