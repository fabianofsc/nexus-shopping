# Structured Logging & Correlation-ID Implementation

**Status:** ✅ Complete  
**Branch:** `feat/structured-logging-correlation-id`  
**Spec:** `docs/superpowers/specs/2026-06-29-structured-logging-correlation-id-design.md`  
**Decision Record:** `docs/decisions/2026-06-29-prd-structured-logging-correlation-id.md`

---

## Summary

This implementation introduces structured logging (ECS/JSON format) and per-request correlation-ID tracking to prepare the Nexus Shopping API for future observability integration with Grafana/Loki.

### Key Features

- **Structured Logging:** ECS/JSON format on stdout (Spring Boot 4 native)
- **Correlation-ID:** Automatic `X-Correlation-ID` header resolution per HTTP request
- **MDC Integration:** SLF4J Mapped Diagnostic Context includes `correlation.id` in all logs
- **Request Summary:** Automatic `http.request.completed` event log per completed request
- **Security:** Safe validation of correlation-IDs (alphanumeric + `.`, `_`, `-`, `:`, max 128 chars)
- **Architecture:** Zero framework dependencies in domain/usecase (adapters only)

---

## Implementation Details

### Components

**1. CorrelationIdProvider** (`src/main/kotlin/com/nexus/shopping/infra/correlation/CorrelationIdProvider.kt`)
- Pure validation class (no Spring dependencies)
- Generates UUID v4 for missing/blank/invalid correlation-IDs
- Validates character set and 128-char maximum
- Regex pattern: `^[a-zA-Z0-9._:\-]*$`

**2. CorrelationIdFilter** (`src/main/kotlin/com/nexus/shopping/infra/http/CorrelationIdFilter.kt`)
- Spring `@Component` servlet filter
- Reads `X-Correlation-ID` header from requests
- Injects resolved ID into MDC as `correlation.id`
- Returns `X-Correlation-ID` header in responses
- Logs `http.request.completed` summary on request completion
- Cleans MDC in finally block (no context leaks)

**3. Configuration** (`src/main/resources/application.yml`)
- `spring.logback.format: ecs` — ECS/JSON output format
- `spring.logback.structured-logging.enabled: true` — Structured logging
- UTF-8 console charset

### HTTP Contract

**Request:**
```http
GET /products?categoryId=1&page=0&size=5
X-Correlation-ID: trace-001-abc
```

**Response:**
```http
HTTP/1.1 200 OK
X-Correlation-ID: trace-001-abc

{"content": [...], "page": 0, ...}
```

**Rules:**
- Missing header → generates UUID
- Blank header → generates UUID
- Invalid characters → generates UUID
- Valid header → preserved
- Max 128 characters enforced

### Log Contract

**Event:** `http.request.completed`

**Fields (ECS format):**
- `@timestamp` — Request completion timestamp
- `log.level` — INFO (2xx-4xx) or ERROR (5xx)
- `service.name` — Application name (Nexus Shopping)
- `message` — "http.request.completed"
- `correlation.id` — Resolved correlation ID
- `http.request.method` — HTTP method (GET, POST, etc.)
- `url.path` — Request path
- `http.response.status_code` — HTTP status code
- `event.duration` — Request duration in milliseconds

**Exclusions:** No body, query string, sensitive headers, stack traces, SQL, or business parameters.

---

## Test Coverage (11/11 ✅)

### CorrelationIdProviderTest (6 tests)
1. Null header generates UUID (36 chars)
2. Blank header generates unique UUID
3. Valid header preserved
4. Invalid characters generate UUID
5. Oversized header (>128 chars) generates UUID
6. Allowed special chars (`.`, `_`, `-`, `:`) preserved

### CorrelationIdFilterTest (5 tests)
1. Missing header returns UUID in response
2. Valid header preserved in response
3. Invalid header returns UUID
4. Oversized header returns UUID
5. MDC cleaned after request completion

**Build Status:** `BUILD SUCCESSFUL`

---

## Acceptance Criteria (10/10 ✅)

- ✅ Application emits structured ECS/JSON logs to console by default
- ✅ All HTTP responses include `X-Correlation-ID` header
- ✅ Requests without correlation-id receive generated UUID
- ✅ Valid correlation-ids are preserved
- ✅ Invalid correlation-ids receive generated UUID
- ✅ MDC contains `correlation.id` during request and is cleaned after
- ✅ Each completed request generates exactly one summary log
- ✅ Summary log excludes body, query string, and business parameters
- ✅ Architectural decision documented in `docs/decisions`
- ✅ Build passes with Gradle Wrapper

---

## Architecture

**Dependency Direction:** Adapters → Application → Domain

```
infra/
  ├── correlation/
  │   └── CorrelationIdProvider.kt    (pure, reusable)
  └── http/
      └── CorrelationIdFilter.kt      (@Component, adapter layer)

application.yml                    (Spring Boot config)
```

**Domain Isolation:** Zero dependencies on logging, MDC, Servlet API, Spring Web, or observability formats.

---

## Future Evolution

### OpenTelemetry Integration
When OpenTelemetry is introduced:
- Accept and propagate W3C `traceparent` header
- Register `trace.id` and `span.id` in logs
- Maintain `correlation.id` for backward compatibility and didactic queries

### Grafana/Loki Stack
When observability infrastructure is added:
- Collect logs from container stdout (no code changes required)
- Aggregate via ECS field mappings
- Query by `correlation.id` for request tracing

### Microservices
When multi-service deployment begins:
- All services follow same correlation-ID contract
- Propagate `X-Correlation-ID` across service boundaries
- Maintain ECS schema consistency

---

## Implementation Notes

- Correlation-ID validation is intentionally conservative (safe for log injection prevention)
- Single log summary per request prevents audit trail bloat
- MDC cleanup in finally block prevents context leaks in connection pools
- CorrelationIdProvider is pure (no framework) for reuse in other adapters (CLI, queues)
- Spring Boot 4 native ECS support chosen over custom Logback configuration for portability

---

**Implemented by:** Claude Haiku 4.5  
**Date:** 2026-06-29  
**Status:** Ready for production
