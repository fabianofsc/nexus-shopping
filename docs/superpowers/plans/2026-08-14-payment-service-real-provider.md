# nexus-payment-service as Sole Payment Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delete the simulated `LoggingPaymentProviderGateway` and its persistence, make the real
`PaymentServiceProviderGateway` (renamed from `NexusPaymentServiceProviderGateway`) the only
`PaymentProviderGateway` implementation, and rewrite every checkout test that assumed synchronous
payment resolution to work against the now-always-async flow via WireMock stubs.

**Architecture:** `payment` keeps exactly one `PaymentProviderGateway` bean, no more
`@ConditionalOnProperty` toggle. `PaymentProvider` becomes a single-value enum
(`PAYMENT_SERVICE`). Checkout HTTP integration tests stand up a WireMock server in-process
(via `wiremock-spring-boot`) bound to `nexus.payment-service.base-url`, so the real gateway code
runs unmodified against a scripted HTTP double instead of a fake Kotlin object. Since dispatch
always returns `PROCESSING`, tests trigger confirmation by calling
`PaymentReconciliationUseCase.reconcile()` directly rather than waiting on the `@Scheduled`
poller (neutralized in tests via a large `polling-interval` in `src/test/resources/application.yml`).
Docker Compose absorbs the whole Payment Service + Dummy Pay topology using published images only
(no local build, no sibling-repo checkout required).

**Tech Stack:** Kotlin, Spring Boot 4, `org.wiremock.integrations:wiremock-spring-boot:4.2.2`
(new test dependency), Flyway, H2 (tests), PostgreSQL, Docker Compose.

**Spec:** `docs/superpowers/specs/2026-08-14-payment-service-real-provider-design.md` — read
before starting.

## Global Constraints

- `payment` and `application` packages keep zero imports of `jakarta.persistence`,
  `org.hibernate`, or `org.springframework.data` outside adapters (existing project rule,
  unaffected by this plan).
- Migrations are immutable once applied — never edit `V1`..`V12`; only add new ones (`V13`).
- `./gradlew build` must stay green using WireMock exclusively — no test may depend on network
  access or the real `nexus-payment-service`/`dummy-pay` actually running.
- Docker Compose must reference published images only (`fabianofsc/nexus-payment-service:latest`,
  `fabianofsc/dummy-pay:latest`) — no local build context, no sibling-repo path dependency.
- Commits grouped by context (code, migrations, tests, docs) per `CLAUDE.md`.
- `CLAUDE.md` must stay under 200 lines after edits.

---

### Task 1: Delete the simulated gateway; make `PaymentServiceProviderGateway` the only implementation

**Files:**

- Modify: `build.gradle.kts` (+wiremock-spring-boot test dependency)
- Create: `src/main/resources/db/migration/V13__drop_payment_provider_dispatches.sql`
- Delete: `payment/adapter/outbound/provider/LoggingPaymentProviderGateway.kt`
- Delete: `payment/adapter/outbound/provider/PaymentProviderDispatchEntity.kt`
- Delete: `payment/adapter/outbound/provider/SpringDataPaymentProviderDispatchRepository.kt`
- Delete: `src/test/kotlin/com/nexus/shopping/payment/adapter/outbound/provider/LoggingPaymentProviderGatewayTest.kt`
- Delete: `src/test/kotlin/com/nexus/shopping/checkout/PaymentCheckoutReconciliationHttpTest.kt`
- Modify: `payment/domain/PaymentProvider.kt` (single value: `PAYMENT_SERVICE`)
- Rename+Modify: `payment/adapter/outbound/provider/NexusPaymentServiceProviderGateway.kt` ->
  `PaymentServiceProviderGateway.kt` (class rename, remove `@ConditionalOnProperty`)
- Rename+Modify: `src/test/kotlin/.../NexusPaymentServiceProviderGatewayTest.kt` ->
  `PaymentServiceProviderGatewayTest.kt`
- Modify: `checkout/adapter/inbound/scheduler/PaymentReconciliationScheduler.kt`
  (remove `@ConditionalOnProperty`)
- Modify: `src/main/resources/application.yml` (remove `enabled` key)
- Modify: `src/test/resources/application.yml` (+`nexus.payment-service.polling-interval:
  600000` to neutralize the scheduler in tests)
- Modify: `src/main/kotlin/com/nexus/shopping/payment/adapter/outbound/jpa/PaymentAttemptEntity.kt`
  (default provider value)
- Modify: `src/main/kotlin/com/nexus/shopping/payment/application/usecase/ReconcilePendingPaymentAttemptsUseCase.kt`
  (enum rename)
- Modify: `src/test/kotlin/com/nexus/shopping/payment/domain/PaymentAttemptTest.kt` (enum rename)
- Modify: `src/test/kotlin/com/nexus/shopping/payment/PaymentMigrationContractTest.kt` (literal
  rename)
- Modify: `src/test/kotlin/com/nexus/shopping/payment/adapter/outbound/jpa/PaymentJpaRepositoryAdapterTest.kt`
  (redesign `findPendingByProvider` test — single provider value left, test REQUESTED vs
  terminal filtering instead of provider-vs-provider filtering)
- Modify: `src/test/kotlin/com/nexus/shopping/payment/ProcessPaymentUseCaseTest.kt` (enum rename)
- Modify: `src/test/kotlin/com/nexus/shopping/payment/ReconcilePendingPaymentAttemptsUseCaseTest.kt`
  (enum rename)
- Rewrite: `src/test/kotlin/com/nexus/shopping/checkout/PaymentCheckoutHttpTest.kt`
- Rewrite: `src/test/kotlin/com/nexus/shopping/checkout/PaymentCheckoutConcurrencyHttpTest.kt`
- Rewrite: `src/test/kotlin/com/nexus/shopping/checkout/PaymentRequestedCheckoutHttpTest.kt`

**Interfaces:**

- Consumes: `PaymentProviderGateway` (unchanged shape — `val provider`, `process`,
  `checkStatus`), `PaymentReconciliationUseCase.reconcile(): Unit` (existing, now called
  directly from tests).
- Produces: `PaymentServiceProviderGateway` — same public shape as the deleted
  `NexusPaymentServiceProviderGateway`, Spring bean name `paymentServiceProviderGateway`.
  `PaymentProvider.PAYMENT_SERVICE` — the only enum value, used by
  `findPendingByProvider(PaymentProvider.PAYMENT_SERVICE)`.

- [ ] **Step 1: Add the WireMock Spring Boot test dependency.**

In `build.gradle.kts`, in the `dependencies` block, add this line right after
`testImplementation("org.springframework.boot:spring-boot-starter-test")`:

```kotlin
    testImplementation("org.wiremock.integrations:wiremock-spring-boot:4.2.2")
```

- [ ] **Step 2: Create the migration dropping the orphaned dispatch journal table.**

Create `src/main/resources/db/migration/V13__drop_payment_provider_dispatches.sql`:

```sql
DROP TABLE payment_provider_dispatches;
```

- [ ] **Step 3: Delete the simulated gateway and its persistence.**

```bash
rm src/main/kotlin/com/nexus/shopping/payment/adapter/outbound/provider/LoggingPaymentProviderGateway.kt
rm src/main/kotlin/com/nexus/shopping/payment/adapter/outbound/provider/PaymentProviderDispatchEntity.kt
rm src/main/kotlin/com/nexus/shopping/payment/adapter/outbound/provider/SpringDataPaymentProviderDispatchRepository.kt
rm src/test/kotlin/com/nexus/shopping/payment/adapter/outbound/provider/LoggingPaymentProviderGatewayTest.kt
rm src/test/kotlin/com/nexus/shopping/checkout/PaymentCheckoutReconciliationHttpTest.kt
```

- [ ] **Step 4: Rename the `PaymentProvider` enum to a single value.**

Replace the full content of `src/main/kotlin/com/nexus/shopping/payment/domain/PaymentProvider.kt`:

```kotlin
package com.nexus.shopping.payment.domain

enum class PaymentProvider {
    PAYMENT_SERVICE,
}
```

- [ ] **Step 5: Rename `NexusPaymentServiceProviderGateway.kt` to `PaymentServiceProviderGateway.kt`.**

```bash
git mv src/main/kotlin/com/nexus/shopping/payment/adapter/outbound/provider/NexusPaymentServiceProviderGateway.kt \
  src/main/kotlin/com/nexus/shopping/payment/adapter/outbound/provider/PaymentServiceProviderGateway.kt
```

Edit the file: rename the class, remove the `@ConditionalOnProperty` line and its now-unused
import, update `override val provider`. Full resulting content:

```kotlin
package com.nexus.shopping.payment.adapter.outbound.provider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.nexus.shopping.payment.application.exception.PaymentProviderGatewayException
import com.nexus.shopping.payment.application.port.outbound.PaymentProviderGateway
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingRequest
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingResult
import com.nexus.shopping.payment.application.port.outbound.ProviderStatusResult
import com.nexus.shopping.payment.domain.PaymentProvider
import com.nexus.shopping.payment.domain.PaymentStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body

@Component
class PaymentServiceProviderGateway(
    restClientBuilder: RestClient.Builder,
    @Value("\${nexus.payment-service.base-url}")
    baseUrl: String,
) : PaymentProviderGateway {
    override val provider = PaymentProvider.PAYMENT_SERVICE

    private val restClient = restClientBuilder.baseUrl(baseUrl).build()
    private val errorMapper = ObjectMapper()

    override fun process(request: ProviderProcessingRequest): ProviderProcessingResult {
        val response =
            invoke {
                restClient
                    .post()
                    .uri("/v1/payments")
                    .header("Idempotency-Key", request.providerDispatchKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        DispatchRequestBody(
                            referenceId = request.referenceId,
                            amount =
                                request.amount.value
                                    .movePointRight(2)
                                    .longValueExact(),
                            paymentToken = request.paymentToken,
                        ),
                    ).retrieve()
                    .body<DispatchResponseBody>()
            }
        val dispatched = requireNotNull(response) { "nexus-payment-service returned an empty dispatch response." }
        return ProviderProcessingResult(
            status = PaymentStatus.REQUESTED,
            providerTransactionId = null,
            providerAttemptReference = dispatched.attemptReference,
        )
    }

    override fun checkStatus(providerAttemptReference: String): ProviderStatusResult {
        val response =
            invoke {
                restClient
                    .get()
                    .uri("/v1/payments/{attemptReference}", providerAttemptReference)
                    .retrieve()
                    .body<StatusResponseBody>()
            }
        val status = requireNotNull(response) { "nexus-payment-service returned an empty status response." }
        return ProviderStatusResult(
            status = status.status.toPaymentStatus(),
            providerTransactionId = null,
        )
    }

    private fun <T> invoke(call: () -> T): T =
        try {
            call()
        } catch (exception: RestClientResponseException) {
            throw PaymentProviderGatewayException(
                "nexus-payment-service returned ${exception.statusCode.value()} (${extractCode(exception)}).",
                exception,
            )
        }

    private fun extractCode(exception: RestClientResponseException): String =
        runCatching {
            errorMapper.readTree(exception.responseBodyAsString).get("code")?.asText()
        }.getOrNull() ?: "unknown"

    private fun String.toPaymentStatus(): PaymentStatus =
        when (this) {
            "APPROVED" -> PaymentStatus.APPROVED
            "REJECTED" -> PaymentStatus.REJECTED
            else -> PaymentStatus.REQUESTED
        }
}

private data class DispatchRequestBody(
    val referenceId: String,
    val amount: Long,
    val paymentToken: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class DispatchResponseBody(
    val attemptReference: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class StatusResponseBody(
    val attemptReference: String,
    val status: String,
)
```

- [ ] **Step 6: Rename the gateway's unit test file.**

```bash
git mv src/test/kotlin/com/nexus/shopping/payment/adapter/outbound/provider/NexusPaymentServiceProviderGatewayTest.kt \
  src/test/kotlin/com/nexus/shopping/payment/adapter/outbound/provider/PaymentServiceProviderGatewayTest.kt
```

Edit the file: rename the class `NexusPaymentServiceProviderGatewayTest` ->
`PaymentServiceProviderGatewayTest`, and every `NexusPaymentServiceProviderGateway(` constructor
call -> `PaymentServiceProviderGateway(`. No other change — the test logic and MockRestServiceServer
setup are unaffected by the rename.

- [ ] **Step 7: Remove the toggle from the scheduler.**

Edit `src/main/kotlin/com/nexus/shopping/checkout/adapter/inbound/scheduler/PaymentReconciliationScheduler.kt`:

```kotlin
package com.nexus.shopping.checkout.adapter.inbound.scheduler

import com.nexus.shopping.checkout.application.usecase.PaymentReconciliationUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PaymentReconciliationScheduler(
    private val reconciliation: PaymentReconciliationUseCase,
) {
    @Scheduled(fixedDelayString = "\${nexus.payment-service.polling-interval:2000}")
    fun reconcile() {
        reconciliation.reconcile()
    }
}
```

- [ ] **Step 8: Remove the `enabled` toggle from application config.**

In `src/main/resources/application.yml`, change:

```yaml
  payment-service:
    enabled: ${NEXUS_PAYMENT_SERVICE_ENABLED:false}
    base-url: ${NEXUS_PAYMENT_SERVICE_BASE_URL:http://localhost:8081}
    polling-interval: ${NEXUS_PAYMENT_SERVICE_POLLING_INTERVAL:2000}
```

to:

```yaml
  payment-service:
    base-url: ${NEXUS_PAYMENT_SERVICE_BASE_URL:http://localhost:8081}
    polling-interval: ${NEXUS_PAYMENT_SERVICE_POLLING_INTERVAL:2000}
```

- [ ] **Step 9: Neutralize the scheduler during tests.**

In `src/test/resources/application.yml`, add a `nexus.payment-service.polling-interval`
override big enough that it never fires within a test run (real reconciliation happens via
explicit `PaymentReconciliationUseCase.reconcile()` calls in test code instead). Full resulting
file:

```yaml
spring:
  cache:
    type: simple
  jpa:
    open-in-view: false

management:
  health:
    redis:
      enabled: false

nexus:
  payment-service:
    polling-interval: 600000
  cache:
    redis:
      enabled: false
```

- [ ] **Step 10: Fix the JPA entity's default provider value.**

In `src/main/kotlin/com/nexus/shopping/payment/adapter/outbound/jpa/PaymentAttemptEntity.kt`,
change:

```kotlin
    var provider: PaymentProvider = PaymentProvider.LOGGING_PROVIDER,
```

to:

```kotlin
    var provider: PaymentProvider = PaymentProvider.PAYMENT_SERVICE,
```

- [ ] **Step 11: Fix the reconciliation use case's enum reference.**

In `src/main/kotlin/com/nexus/shopping/payment/application/usecase/ReconcilePendingPaymentAttemptsUseCase.kt`,
change:

```kotlin
            .findPendingByProvider(PaymentProvider.NEXUS_PAYMENT_SERVICE)
```

to:

```kotlin
            .findPendingByProvider(PaymentProvider.PAYMENT_SERVICE)
```

- [ ] **Step 12: Fix `PaymentAttemptTest.kt`'s enum reference.**

In `src/test/kotlin/com/nexus/shopping/payment/domain/PaymentAttemptTest.kt`, in
`requestedAttempt()`, change `provider = PaymentProvider.LOGGING_PROVIDER,` to
`provider = PaymentProvider.PAYMENT_SERVICE,`.

- [ ] **Step 13: Fix `PaymentMigrationContractTest.kt`'s literal.**

In `src/test/kotlin/com/nexus/shopping/payment/PaymentMigrationContractTest.kt`, in
`insertAttempt`, change the SQL literal `'LOGGING_PROVIDER'` to `'PAYMENT_SERVICE'`.

- [ ] **Step 14: Redesign `PaymentJpaRepositoryAdapterTest.kt`'s provider-filtering test.**

With a single enum value left, "filter by provider" collapses into "filter by status" — replace
the test. In `src/test/kotlin/com/nexus/shopping/payment/adapter/outbound/jpa/PaymentJpaRepositoryAdapterTest.kt`,
replace the whole `findPendingByProvider only returns requested attempts for the given provider`
test with:

```kotlin
    @Test
    fun `findPendingByProvider only returns attempts still in the requested status`() {
        val pending = requested(referenceId = "order-pending", idempotencyKey = "payment-pending")
        val toComplete = requested(referenceId = "order-completed", idempotencyKey = "payment-completed")
        attempts.reserve(pending)
        val completedCreated = assertIs<PaymentAttemptReservation.Created>(attempts.reserve(toComplete)).attempt
        attempts.complete(
            completedCreated.attemptReference,
            toComplete.processingLeaseToken!!,
            PaymentStatus.APPROVED,
            "provider-tx",
            Instant.now(),
        )

        val result = attempts.findPendingByProvider(PaymentProvider.PAYMENT_SERVICE)

        assertEquals(1, result.count { it.referenceId == pending.referenceId })
        assertEquals(0, result.count { it.referenceId == toComplete.referenceId })
    }
```

And simplify the `requested()` helper's default (drop the now-pointless `provider` parameter
since only one value exists):

```kotlin
    private fun requested(
        referenceId: String,
        idempotencyKey: String,
        processingLeaseUntil: Instant = Instant.now().plusSeconds(60),
    ) = PaymentAttempt.requested(
        attemptReference = "pay-${UUID.randomUUID()}",
        referenceId = referenceId,
        amount = PaymentAmount.of("19.90".toBigDecimal()),
        currency = PaymentCurrency.of("BRL"),
        provider = PaymentProvider.PAYMENT_SERVICE,
        idempotencyKey = idempotencyKey,
        authorizationFingerprint = "fingerprint-${UUID.randomUUID()}",
        processingLeaseToken = "lease-${UUID.randomUUID()}",
        processingLeaseUntil = processingLeaseUntil,
        createdAt = Instant.parse("2026-07-26T12:00:00Z"),
    )
```

- [ ] **Step 15: Fix `ProcessPaymentUseCaseTest.kt`'s enum references.**

In `src/test/kotlin/com/nexus/shopping/payment/ProcessPaymentUseCaseTest.kt`:
- In `ApprovedProvider`, change `override val provider = PaymentProvider.LOGGING_PROVIDER` to
  `override val provider = PaymentProvider.PAYMENT_SERVICE`.
- In `RequestedProvider`, change `override val provider = PaymentProvider.NEXUS_PAYMENT_SERVICE`
  to `override val provider = PaymentProvider.PAYMENT_SERVICE`.
- In `keeps the attempt requested and records the provider attempt reference when dispatch is
  still processing`, change
  `assertEquals(PaymentProvider.NEXUS_PAYMENT_SERVICE, repository.attempts.single().provider)`
  to `assertEquals(PaymentProvider.PAYMENT_SERVICE, repository.attempts.single().provider)`.
- In `tags the created attempt with the active gateway's provider identity`, change
  `assertEquals(PaymentProvider.LOGGING_PROVIDER, repository.attempts.single().provider)` to
  `assertEquals(PaymentProvider.PAYMENT_SERVICE, repository.attempts.single().provider)`.

- [ ] **Step 16: Fix `ReconcilePendingPaymentAttemptsUseCaseTest.kt`'s enum references.**

In `src/test/kotlin/com/nexus/shopping/payment/ReconcilePendingPaymentAttemptsUseCaseTest.kt`:
- In `FakeProviderGateway`, change `override val provider = PaymentProvider.NEXUS_PAYMENT_SERVICE`
  to `override val provider = PaymentProvider.PAYMENT_SERVICE`.
- In `seedDispatched`, change `provider = PaymentProvider.NEXUS_PAYMENT_SERVICE,` to
  `provider = PaymentProvider.PAYMENT_SERVICE,`.

- [ ] **Step 17: Run the payment package tests to confirm the rename is complete.**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.payment.*"
```

Expected: fails to compile — `PaymentCheckoutHttpTest.kt`, `PaymentCheckoutConcurrencyHttpTest.kt`,
and `PaymentRequestedCheckoutHttpTest.kt` still reference `LoggingPaymentProviderGateway` and
`payment_provider_dispatches`. This is expected; they're rewritten in the next steps. If any
error is NOT in those three files, stop and fix it before continuing — everything else should
already compile clean.

- [ ] **Step 18: Rewrite `PaymentCheckoutHttpTest.kt`.**

Replace the full content of `src/test/kotlin/com/nexus/shopping/checkout/PaymentCheckoutHttpTest.kt`:

```kotlin
package com.nexus.shopping.checkout

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.nexus.shopping.checkout.application.usecase.PaymentReconciliationUseCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:payment_checkout_http_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@EnableWireMock([ConfigureWireMock(baseUrlProperties = ["nexus.payment-service.base-url"])])
class PaymentCheckoutHttpTest {
    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var reconciliation: PaymentReconciliationUseCase

    @InjectWireMock
    private lateinit var wireMock: WireMockServer

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    @Test
    fun `approved payment confirms Order and sends Notification`() {
        stubDispatch("provider-attempt-1")
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)

        val dispatched = checkout(port, customerId, "approved-${UUID.randomUUID()}", "approved")
        assertEquals(202, dispatched.statusCode())
        val orderId = mapper.readTree(dispatched.body())["id"].asLong()

        stubStatus("provider-attempt-1", "APPROVED")
        reconciliation.reconcile()

        val order = getOrder(port, customerId, orderId)
        assertEquals("CONFIRMED", order["status"].asText())
        assertEquals("APPROVED", scalar("SELECT status FROM payment_attempts WHERE reference_id = ?", "checkout:$orderId"))
        assertEquals("SENT", scalar("SELECT status FROM notifications WHERE reference_id = ?", orderId))
    }

    @Test
    fun `rejected payment fails Order without creating Notification`() {
        stubDispatch("provider-attempt-2")
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)

        val dispatched = checkout(port, customerId, "rejected-${UUID.randomUUID()}", "rejected")
        assertEquals(202, dispatched.statusCode())
        val orderId = mapper.readTree(dispatched.body())["id"].asLong()

        stubStatus("provider-attempt-2", "REJECTED")
        reconciliation.reconcile()

        val order = getOrder(port, customerId, orderId)
        assertEquals("PAYMENT_FAILED", order["status"].asText())
        assertEquals("REJECTED", scalar("SELECT status FROM payment_attempts WHERE reference_id = ?", "checkout:$orderId"))
        assertEquals(0, count("SELECT COUNT(*) FROM notifications WHERE reference_id = ?", orderId))
    }

    @Test
    fun `checkout replay reconciles terminal result without a second provider dispatch or Notification`() {
        stubDispatch("provider-attempt-3")
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)
        val idempotencyKey = "replay-${UUID.randomUUID()}"

        val created = checkout(port, customerId, idempotencyKey, "approved")
        assertEquals(202, created.statusCode())
        val orderId = mapper.readTree(created.body())["id"].asLong()

        stubStatus("provider-attempt-3", "APPROVED")
        reconciliation.reconcile()

        val replay = checkout(port, customerId, idempotencyKey, "approved")

        assertEquals(200, replay.statusCode())
        val replayedOrder = mapper.readTree(replay.body())
        assertEquals(orderId, replayedOrder["id"].asLong())
        assertEquals("CONFIRMED", replayedOrder["status"].asText())
        wireMock.verify(1, postRequestedFor(urlEqualTo("/v1/payments")))
        assertEquals(1, count("SELECT COUNT(*) FROM notifications WHERE reference_id = ?", orderId))
    }

    @Test
    fun `same checkout key with a different token returns conflict without another dispatch`() {
        stubDispatch("provider-attempt-4")
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)
        val idempotencyKey = "token-conflict-${UUID.randomUUID()}"

        val created = checkout(port, customerId, idempotencyKey, "approved")
        val conflict = checkout(port, customerId, idempotencyKey, "different-token")

        assertEquals(202, created.statusCode())
        assertEquals(409, conflict.statusCode())
        wireMock.verify(1, postRequestedFor(urlEqualTo("/v1/payments")))
    }

    @Test
    fun `invalid trusted total rolls back before creating Order or confirming Cart`() {
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId, unitPriceAmount = "9999999999.99", quantity = 2)

        val response = checkout(port, customerId, "invalid-total-${UUID.randomUUID()}", "approved")

        assertEquals(400, response.statusCode())
        assertEquals(0, count("SELECT COUNT(*) FROM orders WHERE customer_id = ?", customerId))
        assertEquals("ACTIVE", scalar("SELECT status FROM carts WHERE customer_id = ?", customerId))
    }

    private fun stubDispatch(providerAttemptReference: String) {
        wireMock.stubFor(
            post(urlEqualTo("/v1/payments"))
                .willReturn(
                    aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """{"attemptReference":"$providerAttemptReference","referenceId":"irrelevant","status":"PROCESSING","replayed":false}""",
                        ),
                ),
        )
    }

    private fun stubStatus(
        providerAttemptReference: String,
        status: String,
    ) {
        wireMock.stubFor(
            get(urlEqualTo("/v1/payments/$providerAttemptReference"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"attemptReference":"$providerAttemptReference","status":"$status"}"""),
                ),
        )
    }

    private fun getOrder(
        port: String,
        customerId: Long,
        orderId: Long,
    ) = mapper.readTree(
        httpClient
            .send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:$port/customers/$customerId/orders/$orderId")).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            ).body(),
    )

    private fun createCustomer(port: String): Long {
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val response =
            post(
                port,
                "/customers",
                """
                {
                  "name": "Payment Customer",
                  "document": "$suffix",
                  "documentType": "CPF",
                  "email": "$suffix@example.com",
                  "street": "Rua Teste",
                  "number": "1",
                  "neighborhood": "Centro",
                  "city": "Sao Paulo",
                  "state": "SP",
                  "zipCode": "01001000",
                  "country": "BR"
                }
                """.trimIndent(),
            )
        assertEquals(201, response.statusCode())
        return mapper.readTree(response.body())["id"].asLong()
    }

    private fun addItem(
        port: String,
        customerId: Long,
        unitPriceAmount: String = "19.90",
        quantity: Int = 2,
    ) {
        val response =
            post(
                port,
                "/customers/$customerId/cart/items",
                """
                {
                  "productId": 10,
                  "productName": "Product 10",
                  "unitPriceAmount": $unitPriceAmount,
                  "currency": "BRL",
                  "quantity": $quantity
                }
                """.trimIndent(),
            )
        assertEquals(200, response.statusCode())
    }

    private fun checkout(
        port: String,
        customerId: Long,
        idempotencyKey: String,
        paymentToken: String,
    ): HttpResponse<String> =
        post(
            port,
            "/customers/$customerId/cart/checkout",
            """
            {
              "customerSnapshot": {
                "name": "Payment Customer",
                "document": "12345678900",
                "documentType": "CPF",
                "email": "payment@example.com",
                "phone": null
              },
              "shippingAddressSnapshot": {
                "street": "Rua Teste",
                "number": "1",
                "complement": null,
                "neighborhood": "Centro",
                "city": "Sao Paulo",
                "state": "SP",
                "zipCode": "01001000",
                "country": "BR"
              },
              "paymentToken": "$paymentToken"
            }
            """.trimIndent(),
            idempotencyKey,
        )

    private fun post(
        port: String,
        path: String,
        body: String,
        idempotencyKey: String? = null,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .header("Content-Type", "application/json")
                .apply {
                    if (idempotencyKey != null) header("Idempotency-Key", idempotencyKey)
                }.POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun scalar(
        sql: String,
        argument: Any,
    ): String = requireNotNull(jdbcTemplate.queryForObject(sql, String::class.java, argument))

    private fun count(
        sql: String,
        argument: Any,
    ): Int = requireNotNull(jdbcTemplate.queryForObject(sql, Int::class.java, argument))
}
```

- [ ] **Step 19: Rewrite `PaymentRequestedCheckoutHttpTest.kt`.**

The artificial `BlockingPaymentProvider` wrapper existed to force the `REQUESTED` path over a
gateway that normally resolved synchronously. That's no longer needed — every checkout now
enters `REQUESTED` naturally. Replace the full content of
`src/test/kotlin/com/nexus/shopping/checkout/PaymentRequestedCheckoutHttpTest.kt`:

```kotlin
package com.nexus.shopping.checkout

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.nexus.shopping.checkout.application.usecase.PaymentReconciliationUseCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:payment_requested_checkout_http_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@EnableWireMock([ConfigureWireMock(baseUrlProperties = ["nexus.payment-service.base-url"])])
class PaymentRequestedCheckoutHttpTest {
    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var reconciliation: PaymentReconciliationUseCase

    @InjectWireMock
    private lateinit var wireMock: WireMockServer

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    @Test
    fun `checkout responds WAITING_PAYMENT immediately then confirms once reconciliation observes an approved status`() {
        stubDispatch("provider-attempt-1")
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)
        val idempotencyKey = "requested-${UUID.randomUUID()}"

        val dispatched = checkout(port, customerId, idempotencyKey)

        assertEquals(202, dispatched.statusCode())
        val order = mapper.readTree(dispatched.body())
        assertEquals("WAITING_PAYMENT", order["status"].asText())
        assertEquals(
            "REQUESTED",
            scalar("SELECT status FROM payment_attempts WHERE reference_id = ?", "checkout:${order["id"].asLong()}"),
        )
        assertEquals(0, count("SELECT COUNT(*) FROM notifications WHERE reference_id = ?", order["id"].asLong()))

        val replayWhileProcessing = checkout(port, customerId, idempotencyKey)
        assertEquals(202, replayWhileProcessing.statusCode())
        assertEquals("WAITING_PAYMENT", mapper.readTree(replayWhileProcessing.body())["status"].asText())

        stubStatus("provider-attempt-1", "APPROVED")
        reconciliation.reconcile()

        val confirmedReplay = checkout(port, customerId, idempotencyKey)
        assertEquals(200, confirmedReplay.statusCode())
        assertEquals("CONFIRMED", mapper.readTree(confirmedReplay.body())["status"].asText())
        assertEquals(1, count("SELECT COUNT(*) FROM notifications WHERE reference_id = ?", order["id"].asLong()))
    }

    private fun stubDispatch(providerAttemptReference: String) {
        wireMock.stubFor(
            post(urlEqualTo("/v1/payments"))
                .willReturn(
                    aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """{"attemptReference":"$providerAttemptReference","referenceId":"irrelevant","status":"PROCESSING","replayed":false}""",
                        ),
                ),
        )
    }

    private fun stubStatus(
        providerAttemptReference: String,
        status: String,
    ) {
        wireMock.stubFor(
            get(urlEqualTo("/v1/payments/$providerAttemptReference"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"attemptReference":"$providerAttemptReference","status":"$status"}"""),
                ),
        )
    }

    private fun createCustomer(port: String): Long {
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val response =
            post(
                port,
                "/customers",
                """
                {
                  "name": "Requested Customer",
                  "document": "$suffix",
                  "documentType": "CPF",
                  "email": "$suffix@example.com",
                  "street": "Rua Teste",
                  "number": "1",
                  "neighborhood": "Centro",
                  "city": "Sao Paulo",
                  "state": "SP",
                  "zipCode": "01001000",
                  "country": "BR"
                }
                """.trimIndent(),
            )
        assertEquals(201, response.statusCode())
        return mapper.readTree(response.body())["id"].asLong()
    }

    private fun addItem(
        port: String,
        customerId: Long,
    ) {
        assertEquals(
            200,
            post(
                port,
                "/customers/$customerId/cart/items",
                """
                {
                  "productId": 10,
                  "productName": "Product 10",
                  "unitPriceAmount": 19.90,
                  "currency": "BRL",
                  "quantity": 2
                }
                """.trimIndent(),
            ).statusCode(),
        )
    }

    private fun checkout(
        port: String,
        customerId: Long,
        idempotencyKey: String,
    ): HttpResponse<String> =
        post(
            port,
            "/customers/$customerId/cart/checkout",
            """
            {
              "customerSnapshot": {
                "name": "Requested Customer",
                "document": "12345678900",
                "documentType": "CPF",
                "email": "requested@example.com",
                "phone": null
              },
              "shippingAddressSnapshot": {
                "street": "Rua Teste",
                "number": "1",
                "complement": null,
                "neighborhood": "Centro",
                "city": "Sao Paulo",
                "state": "SP",
                "zipCode": "01001000",
                "country": "BR"
              },
              "paymentToken": "approved"
            }
            """.trimIndent(),
            idempotencyKey,
        )

    private fun post(
        port: String,
        path: String,
        body: String,
        idempotencyKey: String? = null,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .header("Content-Type", "application/json")
                .apply {
                    if (idempotencyKey != null) header("Idempotency-Key", idempotencyKey)
                }.POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun scalar(
        sql: String,
        argument: Any,
    ): String = requireNotNull(jdbcTemplate.queryForObject(sql, String::class.java, argument))

    private fun count(
        sql: String,
        argument: Any,
    ): Int = requireNotNull(jdbcTemplate.queryForObject(sql, Int::class.java, argument))
}
```

- [ ] **Step 20: Rewrite `PaymentCheckoutConcurrencyHttpTest.kt`.**

Dispatch always returns `REQUESTED` now, so every concurrent response ends up `202`/
`WAITING_PAYMENT` (not a mix of `200`/`201` `CONFIRMED`). The `payment_provider_dispatches`
assertion is replaced by a WireMock request-count verification. Replace the full content of
`src/test/kotlin/com/nexus/shopping/checkout/PaymentCheckoutConcurrencyHttpTest.kt`:

```kotlin
package com.nexus.shopping.checkout

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.nexus.shopping.payment.adapter.outbound.jpa.PaymentJpaRepositoryAdapter
import com.nexus.shopping.payment.adapter.outbound.provider.PaymentServiceProviderGateway
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptRepositoryPort
import com.nexus.shopping.payment.application.port.outbound.PaymentAttemptReservation
import com.nexus.shopping.payment.application.port.outbound.PaymentProviderGateway
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingRequest
import com.nexus.shopping.payment.application.port.outbound.ProviderProcessingResult
import com.nexus.shopping.payment.application.port.outbound.ProviderStatusResult
import com.nexus.shopping.payment.domain.PaymentAttempt
import com.nexus.shopping.payment.domain.PaymentProvider
import com.nexus.shopping.payment.domain.PaymentStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:payment_checkout_concurrency_http_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@EnableWireMock([ConfigureWireMock(baseUrlProperties = ["nexus.payment-service.base-url"])])
@Import(PaymentCheckoutConcurrencyHttpTest.ConcurrencyConfiguration::class)
class PaymentCheckoutConcurrencyHttpTest {
    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var provider: ConcurrentBlockingPaymentProvider

    @Autowired
    private lateinit var attempts: ObservingPaymentAttemptRepository

    @InjectWireMock
    private lateinit var wireMock: WireMockServer

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    @Test
    fun `concurrent identical checkouts produce one dispatch and consistent WAITING_PAYMENT responses`() {
        wireMock.stubFor(
            post(urlEqualTo("/v1/payments"))
                .willReturn(
                    aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """{"attemptReference":"provider-attempt-1","referenceId":"irrelevant","status":"PROCESSING","replayed":false}""",
                        ),
                ),
        )
        val port = environment.getRequiredProperty("local.server.port")
        val customerId = createCustomer(port)
        addItem(port, customerId)
        val idempotencyKey = "concurrent-${UUID.randomUUID()}"
        val start = CyclicBarrier(REQUEST_COUNT)
        val executor = Executors.newFixedThreadPool(REQUEST_COUNT)

        try {
            val responses =
                List(REQUEST_COUNT) {
                    executor.submit<HttpResponse<String>> {
                        start.await(10, TimeUnit.SECONDS)
                        checkout(port, customerId, idempotencyKey)
                    }
                }
            assertTrue(provider.entered.await(10, TimeUnit.SECONDS), "Provider was not invoked")
            assertTrue(attempts.requestedReplays.await(10, TimeUnit.SECONDS), "Concurrent replays did not observe REQUESTED")
            provider.release.countDown()

            val completed = responses.map { it.get(15, TimeUnit.SECONDS) }
            assertEquals(List(REQUEST_COUNT) { 202 }, completed.map { it.statusCode() })
            val orders = completed.map { mapper.readTree(it.body()) }
            assertEquals(setOf("WAITING_PAYMENT"), orders.map { it["status"].asText() }.toSet())
            assertEquals(1, orders.map { it["id"].asLong() }.toSet().size)
            val orderId = orders.first()["id"].asLong()
            wireMock.verify(1, postRequestedFor(urlEqualTo("/v1/payments")))
            assertEquals(1, count("SELECT COUNT(*) FROM payment_attempts WHERE reference_id = ?", "checkout:$orderId"))
        } finally {
            provider.release.countDown()
            executor.shutdownNow()
        }
    }

    private fun createCustomer(port: String): Long {
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val response =
            post(
                port,
                "/customers",
                """
                {
                  "name": "Concurrent Customer",
                  "document": "$suffix",
                  "documentType": "CPF",
                  "email": "$suffix@example.com",
                  "street": "Rua Teste",
                  "number": "1",
                  "neighborhood": "Centro",
                  "city": "Sao Paulo",
                  "state": "SP",
                  "zipCode": "01001000",
                  "country": "BR"
                }
                """.trimIndent(),
            )
        assertEquals(201, response.statusCode())
        return mapper.readTree(response.body())["id"].asLong()
    }

    private fun addItem(
        port: String,
        customerId: Long,
    ) {
        assertEquals(
            200,
            post(
                port,
                "/customers/$customerId/cart/items",
                """
                {
                  "productId": 10,
                  "productName": "Product 10",
                  "unitPriceAmount": 19.90,
                  "currency": "BRL",
                  "quantity": 2
                }
                """.trimIndent(),
            ).statusCode(),
        )
    }

    private fun checkout(
        port: String,
        customerId: Long,
        idempotencyKey: String,
    ): HttpResponse<String> =
        post(
            port,
            "/customers/$customerId/cart/checkout",
            """
            {
              "customerSnapshot": {
                "name": "Concurrent Customer",
                "document": "12345678900",
                "documentType": "CPF",
                "email": "concurrent@example.com",
                "phone": null
              },
              "shippingAddressSnapshot": {
                "street": "Rua Teste",
                "number": "1",
                "complement": null,
                "neighborhood": "Centro",
                "city": "Sao Paulo",
                "state": "SP",
                "zipCode": "01001000",
                "country": "BR"
              },
              "paymentToken": "approved"
            }
            """.trimIndent(),
            idempotencyKey,
        )

    private fun post(
        port: String,
        path: String,
        body: String,
        idempotencyKey: String? = null,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .header("Content-Type", "application/json")
                .apply {
                    if (idempotencyKey != null) header("Idempotency-Key", idempotencyKey)
                }.POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun count(
        sql: String,
        argument: Any,
    ): Int = requireNotNull(jdbcTemplate.queryForObject(sql, Int::class.java, argument))

    @TestConfiguration
    class ConcurrencyConfiguration {
        @Bean
        @Primary
        fun observingPaymentAttempts(
            @Qualifier("paymentJpaRepositoryAdapter")
            delegate: PaymentJpaRepositoryAdapter,
        ): ObservingPaymentAttemptRepository = ObservingPaymentAttemptRepository(delegate, REQUEST_COUNT - 1)

        @Bean
        @Primary
        fun concurrentBlockingPaymentProvider(
            @Qualifier("paymentServiceProviderGateway")
            delegate: PaymentServiceProviderGateway,
        ): ConcurrentBlockingPaymentProvider = ConcurrentBlockingPaymentProvider(delegate)
    }

    private companion object {
        private const val REQUEST_COUNT = 8
    }
}

class ObservingPaymentAttemptRepository(
    private val delegate: PaymentAttemptRepositoryPort,
    replayCount: Int,
) : PaymentAttemptRepositoryPort {
    val requestedReplays = CountDownLatch(replayCount)

    override fun reserve(attempt: PaymentAttempt): PaymentAttemptReservation =
        delegate.reserve(attempt).also { reservation ->
            if (reservation is PaymentAttemptReservation.Existing && reservation.attempt.status == PaymentStatus.REQUESTED) {
                requestedReplays.countDown()
            }
        }

    override fun findByReferenceIdAndIdempotencyKey(
        referenceId: String,
        idempotencyKey: String,
    ): PaymentAttempt? = delegate.findByReferenceIdAndIdempotencyKey(referenceId, idempotencyKey)

    override fun complete(
        attemptReference: String,
        processingLeaseToken: String,
        status: PaymentStatus,
        providerTransactionId: String?,
        completedAt: Instant,
    ): PaymentAttempt? =
        delegate.complete(
            attemptReference,
            processingLeaseToken,
            status,
            providerTransactionId,
            completedAt,
        )

    override fun recordProviderDispatch(
        attemptReference: String,
        processingLeaseToken: String,
        providerAttemptReference: String,
    ): PaymentAttempt? = delegate.recordProviderDispatch(attemptReference, processingLeaseToken, providerAttemptReference)

    override fun findPendingByProvider(
        provider: PaymentProvider,
        limit: Int,
    ): List<PaymentAttempt> = delegate.findPendingByProvider(provider, limit)
}

class ConcurrentBlockingPaymentProvider(
    private val delegate: PaymentProviderGateway,
) : PaymentProviderGateway {
    override val provider = delegate.provider

    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)

    override fun process(request: ProviderProcessingRequest): ProviderProcessingResult {
        entered.countDown()
        check(release.await(10, TimeUnit.SECONDS)) { "Timed out waiting to release provider" }
        return delegate.process(request)
    }

    override fun checkStatus(providerAttemptReference: String): ProviderStatusResult = delegate.checkStatus(providerAttemptReference)
}
```

- [ ] **Step 21: Run the full build.**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Expected: `BUILD SUCCESSFUL`. If `@EnableWireMock([ConfigureWireMock(...)])` or
`@InjectWireMock` fail to compile with an array-literal syntax error, try the single-value form
`@EnableWireMock(ConfigureWireMock(...))` instead — both are plausible Kotlin/Java annotation
interop spellings; whichever compiles is correct, this is a syntax detail with no behavioral
difference.

- [ ] **Step 22: Run ktlint and fix any formatting issues.**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintFormat
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintCheck
```

- [ ] **Step 23: Commit.**

```bash
git add build.gradle.kts src/main src/test
git commit -m "feat: make nexus-payment-service the sole payment provider"
```

---

### Task 2: Docker Compose — bring up the Payment Service topology from published images

**Files:**

- Modify: `docker-compose.yml`

**Interfaces:**

- Consumes: `fabianofsc/nexus-payment-service:latest`, `fabianofsc/dummy-pay:latest` (published
  images, out of scope for this repo per the spec).
- Produces: `nexus-payment-service` reachable at `http://nexus-payment-service:8081` from the
  `backend` network, consumed by `x-app`'s `NEXUS_PAYMENT_SERVICE_BASE_URL`.

- [ ] **Step 1: Add the Payment Service topology to `docker-compose.yml`.**

Replace the full content of `docker-compose.yml`:

```yaml
# Bloco de configuracao compartilhado pelas 3 instancias da aplicacao.
# Campos "x-" sao ignorados pelo compose e servem apenas como ancora YAML,
# garantindo que app1/app2/app3 saiam do mesmo build e da mesma imagem.
x-app: &app-base
  build:
    context: .
    dockerfile: Dockerfile
  image: ${APP_IMAGE:-nexus-shopping:local}
  environment:
    DB_URL: ${DB_URL:-jdbc:postgresql://postgres:5432/nexus_shopping}
    DB_USERNAME: ${DB_USERNAME:-nexus}
    DB_PASSWORD: ${DB_PASSWORD:-nexus}
    PRODUCT_SEED_COUNT: ${PRODUCT_SEED_COUNT:-1000}
    SPRING_DATA_REDIS_HOST: redis
    SPRING_DATA_REDIS_PORT: 6379
    NEXUS_PAYMENT_SERVICE_BASE_URL: ${NEXUS_PAYMENT_SERVICE_BASE_URL:-http://nexus-payment-service:8081}
  expose:
    - "8080"
  depends_on:
    postgres:
      condition: service_healthy
    redis:
      condition: service_healthy
    nexus-payment-service:
      condition: service_started
  networks:
    - backend
  restart: unless-stopped

services:
  app1:
    <<: *app-base
    hostname: app1

  app2:
    <<: *app-base
    hostname: app2

  app3:
    <<: *app-base
    hostname: app3

  nginx:
    image: nginx:stable-alpine
    ports:
      - "8080:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - app1
      - app2
      - app3
    networks:
      - backend
    restart: unless-stopped

  postgres:
    image: postgres:16-alpine
    container_name: nexus-shopping-postgres
    environment:
      POSTGRES_DB: nexus_shopping
      POSTGRES_USER: nexus
      POSTGRES_PASSWORD: nexus
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U nexus -d nexus_shopping"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - backend
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # --- Payment Service (dependencia obrigatoria de runtime a partir desta versao) ---
  # Sobe inteiramente a partir de imagens publicadas: nao requer clonar
  # nexus-payment-service nem dummy-pay localmente.
  nexus-payment-service:
    image: fabianofsc/nexus-payment-service:latest
    environment:
      DB_URL: jdbc:postgresql://nexus-payment-postgres:5432/nexus_payment
      DB_USERNAME: nexus
      DB_PASSWORD: nexus
      NEXUS_PAYMENT_DUMMYPAY_BASE_URL: http://dummypay:8080
      SERVER_PORT: "8081"
      NEXUS_PAYMENT_AUTHORIZATION_FINGERPRINT_SECRET: ${NEXUS_PAYMENT_AUTHORIZATION_FINGERPRINT_SECRET:-dev-secret-change-in-production}
      NEXUS_PAYMENT_DUMMYPAY_KEY_ID: ${NEXUS_PAYMENT_DUMMYPAY_KEY_ID:-local-dev-account}
      NEXUS_PAYMENT_DUMMYPAY_KEY_SECRET: ${NEXUS_PAYMENT_DUMMYPAY_KEY_SECRET:-change-me-to-a-long-random-value}
      NEXUS_PAYMENT_DUMMYPAY_WEBHOOK_SECRET: ${NEXUS_PAYMENT_DUMMYPAY_WEBHOOK_SECRET:-dev-webhook-secret}
    depends_on:
      nexus-payment-postgres:
        condition: service_healthy
      dummypay:
        condition: service_started
    networks:
      - backend
    restart: unless-stopped

  nexus-payment-postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: nexus_payment
      POSTGRES_USER: nexus
      POSTGRES_PASSWORD: nexus
    volumes:
      - nexus-payment-pgdata:/var/lib/postgresql/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U nexus -d nexus_payment"]
      interval: 2s
      timeout: 2s
      retries: 15

  dummypay-postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: dummypay
      POSTGRES_USER: dummypay
      POSTGRES_PASSWORD: dummypay
    volumes:
      - dummypay-pgdata:/var/lib/postgresql/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dummypay -d dummypay"]
      interval: 2s
      timeout: 2s
      retries: 15

  dummypay:
    image: fabianofsc/dummy-pay:latest
    environment:
      DUMMYPAY_HTTP_ADDR: ":8080"
      DUMMYPAY_DATABASE_URL: "postgres://dummypay:dummypay@dummypay-postgres:5432/dummypay?sslmode=disable"
      DUMMYPAY_ACCOUNT_KEY_ID: ${NEXUS_PAYMENT_DUMMYPAY_KEY_ID:-local-dev-account}
      DUMMYPAY_ACCOUNT_KEY_SECRET: ${NEXUS_PAYMENT_DUMMYPAY_KEY_SECRET:-change-me-to-a-long-random-value}
      DUMMYPAY_WEBHOOK_SECRET_ENC_KEY: "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
      DUMMYPAY_PROCESSING_DELAY: "3s"
      DUMMYPAY_IDEMPOTENCY_LEASE: "30s"
      DUMMYPAY_WORKER_POLL_INTERVAL: "250ms"
      DUMMYPAY_WEBHOOK_TIMEOUT: "5s"
    depends_on:
      dummypay-postgres:
        condition: service_healthy
    networks:
      - backend

networks:
  backend:
    driver: bridge

volumes:
  postgres-data:
  nexus-payment-pgdata:
  dummypay-pgdata:
```

Note: neither `nexus-payment-service` nor `dummypay` publish a host port — both stay reachable
only inside the `backend` network, avoiding a port-8080 collision with `nginx`. This means
`curl localhost:8081` from the host won't work against this compose stack (unlike the manual
verification done directly against the sibling repos' own compose files); use
`docker compose exec app1 curl http://nexus-payment-service:8081/actuator/health` or similar if
manual poking from inside the network is needed.

- [ ] **Step 2: Validate the compose file parses correctly.**

```bash
docker compose config --quiet
```

Expected: no output, exit code 0. This only validates YAML/interpolation syntax — it does not
pull or start anything, and does not require `fabianofsc/nexus-payment-service:latest` to exist
yet.

- [ ] **Step 3: Commit.**

```bash
git add docker-compose.yml
git commit -m "feat: bring up nexus-payment-service and dummy-pay in docker-compose"
```

---

### Task 3: Documentation — reflect the real payment integration

**Files:**

- Modify: `CLAUDE.md`
- Modify: `README.md`
- Modify: `docs/agents/external-services.md`

- [ ] **Step 1: Update `CLAUDE.md`'s stack line and Docker Compose note.**

In `CLAUDE.md`, change:

```
- Stack: Kotlin, Java 21, Gradle Wrapper, Spring Boot 4, Actuator, Flyway, PostgreSQL, Spring Data JPA.
```

to:

```
- Stack: Kotlin, Java 21, Gradle Wrapper, Spring Boot 4, Actuator, Flyway, PostgreSQL, Spring Data JPA, WireMock (testes de integracao HTTP).
- Dependencia obrigatoria de runtime: `nexus-payment-service` (imagem `fabianofsc/nexus-payment-service:latest`), unico provider de pagamento.
```

And change:

```
- Docker Compose: `docker compose up -d postgres` / `docker compose down -v`
```

to:

```
- Docker Compose: `docker compose up -d postgres` para so o banco; `docker compose up -d` sobe tambem `nexus-payment-service` + `dummy-pay` (imagens publicadas, sem clone extra).
```

- [ ] **Step 2: Update `README.md`'s Payment extraction bullet.**

In `README.md`, in the "Decisoes principais" list, change:

```
- `Payment` continua sendo a primeira fronteira de extracao. O PSP DummyPay ja existe como servico externo; o proximo passo e criar o Payment Service que o consome antes de refatorar o Nexus.
```

to:

```
- `Payment` foi extraido: o Nexus consome o `nexus-payment-service` real via HTTP (ports/ACL), unico provider de pagamento — o adapter simulado local foi removido. O `nexus-payment-service`, por sua vez, e quem fala com o PSP DummyPay; o Nexus nunca chama DummyPay diretamente.
```

- [ ] **Step 3: Update `README.md`'s "Servicos externos autonomos" section.**

In `README.md`, replace this paragraph:

```
Dois servicos Go ja foram implementados em repositorios separados, com banco,
credenciais, ciclo de vida e contrato HTTP proprios. Eles ainda **nao sao
chamados pelo runtime do Nexus**; portanto, nao ha dependencia de codigo,
submodulo, tabela ou banco compartilhado.
```

with:

```
Dois servicos Go ja foram implementados em repositorios separados, com banco,
credenciais, ciclo de vida e contrato HTTP proprios. O Payment Service
(`nexus-payment-service`) ja e chamado pelo runtime do Nexus via HTTP/ACL,
como unico provider de pagamento; DummyPay continua sendo falado apenas pelo
Payment Service, nunca diretamente pelo Nexus. Nao ha dependencia de codigo,
submodulo, tabela ou banco compartilhado entre os repositorios.
```

And update the table row:

```
| DummyPay | PSP deterministico para vendas com cartao | Implementado; sera consumido pelo futuro Payment Service, nunca diretamente pelo Nexus. |
```

to:

```
| DummyPay | PSP deterministico para vendas com cartao | Implementado; consumido pelo Payment Service, nunca diretamente pelo Nexus. |
```

- [ ] **Step 4: Update `README.md`'s "Executar localmente" section.**

In `README.md`, right after the "Executar localmente" heading's first code block
(`docker compose up -d postgres redis` / `./gradlew bootRun`), add:

```markdown
A partir desta versao, `nexus-payment-service` e uma dependencia obrigatoria de runtime — sem
ele, o checkout falha ao tentar despachar o pagamento. `docker compose up -d` (sem especificar
servicos) sobe a stack completa, incluindo Payment Service e Dummy Pay, a partir de imagens
publicadas.
```

- [ ] **Step 5: Update `docs/agents/external-services.md`'s state and evolution sequence.**

In `docs/agents/external-services.md`, replace this paragraph:

```
Eles nao estao integrados ao runtime deste repositorio. O Nexus continua um
monolito modular Kotlin com Payment e Notification locais; nenhuma chamada HTTP
para esses servicos, dependencia de codigo, submodulo ou acesso cruzado a banco
foi introduzido.
```

with:

```
DummyPay ainda nao esta integrado ao runtime deste repositorio; e falado
exclusivamente pelo Payment Service. O Nexus **ja** consome o Payment Service
(`nexus-payment-service`) real via HTTP/ACL para processar pagamentos — o
adapter simulado local foi removido. Notification Service segue nao
integrado; o Nexus continua com Notification local ate essa etapa futura.
Nenhuma dependencia de codigo, submodulo ou acesso cruzado a banco foi
introduzida em nenhum dos dois casos.
```

And in "Sequencia de evolucao", mark step 3 as done by changing:

```
3. Refatorar o Nexus para substituir o provider de Payment local pelo adapter
   HTTP do Payment Service, preservando o contrato de checkout.
4. Extrair o consumo de notificacao para o Notification Service por adapter/ACL
   proprio, sem acoplamento ao dominio de notificacao generico.
```

to:

```
3. ~~Refatorar o Nexus para substituir o provider de Payment local pelo adapter
   HTTP do Payment Service, preservando o contrato de checkout.~~ Feito.
4. Extrair o consumo de notificacao para o Notification Service por adapter/ACL
   proprio, sem acoplamento ao dominio de notificacao generico.
```

- [ ] **Step 6: Commit.**

```bash
git add CLAUDE.md README.md docs/agents/external-services.md
git commit -m "docs: reflect nexus-payment-service as the sole payment provider"
```

---

### Task 4: Final verification and push

**Files:** none (verification only).

- [ ] **Step 1: Full build from clean.**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`, zero references to `LoggingPaymentProviderGateway`,
`PaymentProviderDispatchEntity`, or `PaymentProvider.LOGGING_PROVIDER` anywhere in the tree.

- [ ] **Step 2: Confirm no dead references remain.**

```bash
grep -rl "LoggingPaymentProviderGateway\|LOGGING_PROVIDER\|NEXUS_PAYMENT_SERVICE\|NexusPaymentServiceProviderGateway\|payment_provider_dispatches" src/
```

Expected: no output (empty match set). If anything prints, it was missed in Task 1 — fix it and
rerun the full build.

- [ ] **Step 3: ktlint check.**

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintCheck
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Push to update PR #27.**

```bash
git push
```

**Stop condition:** This continues PR #27 (same branch, already open) — no new PR to create.
Report the updated PR to the user and wait for their review; never merge without explicit
confirmation for that specific merge.
