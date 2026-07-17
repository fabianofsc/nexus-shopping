# Notification Bounded Context Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar o bounded context `Notification` (dominio, ports, use cases, migration, adapter JPA e endpoint HTTP) para disparo, consulta e listagem paginada de notificacoes de e-mail simuladas, seguindo a spec `docs/superpowers/specs/2026-07-17-notification-context-design.md`.

**Architecture:** Hexagonal (Ports and Adapters), espelhando exatamente os contextos `customer` e `product` existentes: `domain/` puro sem imports de framework, `application/{command,port/outbound,usecase,exception}` orquestrando validacao e regras, `adapter/{inbound/http,outbound/jpa,outbound/email}` traduzindo entre HTTP/JPA e o dominio. O envio de e-mail e simulado via `EmailSenderPort` + `LoggingEmailSenderAdapter` (apenas loga). A listagem paginada usa o mesmo padrao `Slice` sem `COUNT(*)` ja usado em `/products`.

**Tech Stack:** Kotlin 2.2.21, Java 21, Spring Boot 4.1, Spring Data JPA, Hibernate, Flyway, H2 (testes), PostgreSQL (runtime), Gradle Wrapper, kotlin-test-junit5.

## Global Constraints

- `domain/` e `application/` sem imports de `jakarta.persistence`, `org.hibernate` ou `org.springframework.data`.
- Validacao vive no use case; o adapter nao valida.
- Leituras JPA usam `@Query` JPQL explicito; sem derived queries.
- Paginacao via `Slice` sem `COUNT(*)`.
- Migrations portaveis entre PostgreSQL e H2 (sem tipos exclusivos de Postgres).
- Excecoes de aplicacao especificas do contexto estendem as bases de `platform.application.exception` (`ValidationException`, `NotFoundException`).
- `ApiExceptionHandler` (`platform/adapter/inbound/http/ApiExceptionHandler.kt`) e generico: nao deve ser modificado e nao pode importar classes de nenhum contexto (`product`, `customer`, `notification`).
- Nao criar pacote `shared` nem qualquer `package com.nexus.shopping.shared`/`import com.nexus.shopping.shared` (`PackageStructureArchitectureTest` falha se existir).
- Todo comentario de code review (corpo e comentarios inline) deve terminar com uma linha de assinatura da ferramenta que o gerou: `🤖 Generated with Claude Code` ou `🤖 Generated with Codex`.

---

## File Structure

- Create: `src/main/kotlin/com/nexus/shopping/notification/domain/Notification.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationType.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationChannel.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationStatus.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationPage.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationMessageRenderer.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/exception/NotificationNotFoundException.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/exception/NotificationValidationException.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/command/SendNotificationCommand.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/port/outbound/NotificationRepositoryPort.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/port/outbound/EmailSenderPort.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/usecase/NotificationUseCaseLogging.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/usecase/SendNotificationUseCase.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/usecase/GetNotificationByIdUseCase.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/usecase/ListNotificationsByCustomerUseCase.kt`
- Create: `src/main/resources/db/migration/V6__create_notification_context.sql`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/NotificationEntity.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/SpringDataNotificationRepository.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/NotificationJpaRepositoryAdapter.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/email/LoggingEmailSenderAdapter.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/dto/SendNotificationRequest.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/dto/NotificationResponse.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/dto/NotificationPageResponse.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/NotificationController.kt`
- Create: `src/test/kotlin/com/nexus/shopping/notification/domain/NotificationMessageRendererTest.kt`
- Create: `src/test/kotlin/com/nexus/shopping/notification/SendNotificationUseCaseTest.kt`
- Create: `src/test/kotlin/com/nexus/shopping/notification/GetNotificationByIdUseCaseTest.kt`
- Create: `src/test/kotlin/com/nexus/shopping/notification/ListNotificationsByCustomerUseCaseTest.kt`
- Create: `src/test/kotlin/com/nexus/shopping/notification/NotificationMigrationContractTest.kt`
- Create: `src/test/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/NotificationEntityTest.kt`
- Create: `src/test/kotlin/com/nexus/shopping/notification/NotificationControllerTest.kt`
- Modify: `src/test/kotlin/com/nexus/shopping/PackageStructureArchitectureTest.kt`

**Nota (desvio deliberado, resolvido a favor da convencao existente):** a spec descreve `NotificationRepositoryPort.findByCustomerId` retornando `List<Notification>` ("size+1 para hasNext, sem COUNT" como comentario). O padrao real ja estabelecido em `ProductRepositoryPort`/`ProductJpaRepositoryAdapter` retorna um tipo de pagina de dominio (`ProductPage`) calculado a partir de um `Slice` do Spring Data. Este plano segue o padrao existente do `Product`: cria `NotificationPage` (mirror de `ProductPage`) e `NotificationRepositoryPort.findByCustomerId` retorna `NotificationPage`, nao `List<Notification>`. O comportamento (`size + 1` linhas lidas via `Slice`, sem `COUNT(*)`) e identico ao pedido pela spec; apenas o tipo de retorno do port muda para bater com a convencao do codebase, conforme instruido ("resolva conflitos a favor da convencao existente").

**Nota (arquivo nao listado na spec, necessario por convencao existente):** `NotificationUseCaseLogging.kt` (extensions `Logger.infoWithContext`/`Logger.warnWithContext`) nao aparece na spec, mas `customer` e `product` tem cada um sua propria copia `internal` desse helper (nao pode ser compartilhado — `PackageStructureArchitectureTest` proibe pacote `shared`). Este plano cria a copia equivalente para `notification`.

---

## Task 1: Dominio e message renderer

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationType.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationChannel.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationStatus.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/domain/Notification.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationPage.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationMessageRenderer.kt`
- Test: `src/test/kotlin/com/nexus/shopping/notification/domain/NotificationMessageRendererTest.kt`

**Interfaces:**
- Consumes: nada (primeira task do contexto).
- Produces:
  - `enum class NotificationType { ORDER_CONFIRMED, ORDER_PAYMENT_FAILED, ORDER_CANCELLED }`
  - `enum class NotificationChannel { EMAIL }`
  - `enum class NotificationStatus { SENT, FAILED }`
  - `data class Notification(id: Long?, customerId: Long, recipientEmail: String, type: NotificationType, channel: NotificationChannel, status: NotificationStatus, subject: String, body: String, referenceId: Long?, createdAt: Instant?, sentAt: Instant?)`
  - `data class NotificationPage(content: List<Notification>, page: Int, size: Int, count: Int, hasNext: Boolean)`
  - `data class RenderedMessage(subject: String, body: String)`
  - `object NotificationMessageRenderer { fun requiredPlaceholders(type: NotificationType): Set<String>; fun render(type: NotificationType, params: Map<String, String>): RenderedMessage }`

- [ ] **Step 1: Escrever o teste falhando do renderer**

Criar `src/test/kotlin/com/nexus/shopping/notification/domain/NotificationMessageRendererTest.kt`:

```kotlin
package com.nexus.shopping.notification.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationMessageRendererTest {
    @Test
    fun `requiredPlaceholders returns orderId and amount for ORDER_CONFIRMED`() {
        val placeholders = NotificationMessageRenderer.requiredPlaceholders(NotificationType.ORDER_CONFIRMED)

        assertEquals(setOf("orderId", "amount"), placeholders)
    }

    @Test
    fun `requiredPlaceholders returns only orderId for ORDER_CANCELLED`() {
        val placeholders = NotificationMessageRenderer.requiredPlaceholders(NotificationType.ORDER_CANCELLED)

        assertEquals(setOf("orderId"), placeholders)
    }

    @Test
    fun `render substitutes placeholders for ORDER_CONFIRMED`() {
        val message =
            NotificationMessageRenderer.render(
                NotificationType.ORDER_CONFIRMED,
                mapOf("orderId" to "123", "amount" to "99.90"),
            )

        assertEquals("Pedido 123 confirmado", message.subject)
        assertEquals("Seu pedido 123 no valor de 99.90 foi confirmado.", message.body)
    }

    @Test
    fun `render keeps placeholder token when param is missing`() {
        val message = NotificationMessageRenderer.render(NotificationType.ORDER_CANCELLED, emptyMap())

        assertEquals("Pedido {orderId} cancelado", message.subject)
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar a falha**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.domain.NotificationMessageRendererTest"`
Expected: FAIL com `Unresolved reference: NotificationMessageRenderer` (ou erro de compilacao equivalente, pois nenhum tipo do dominio existe ainda).

- [ ] **Step 3: Criar os enums e o modelo de dominio**

Criar `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationType.kt`:

```kotlin
package com.nexus.shopping.notification.domain

enum class NotificationType {
    ORDER_CONFIRMED,
    ORDER_PAYMENT_FAILED,
    ORDER_CANCELLED,
}
```

Criar `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationChannel.kt`:

```kotlin
package com.nexus.shopping.notification.domain

enum class NotificationChannel {
    EMAIL,
}
```

Criar `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationStatus.kt`:

```kotlin
package com.nexus.shopping.notification.domain

enum class NotificationStatus {
    SENT,
    FAILED,
}
```

Criar `src/main/kotlin/com/nexus/shopping/notification/domain/Notification.kt`:

```kotlin
package com.nexus.shopping.notification.domain

import java.time.Instant

data class Notification(
    val id: Long?,
    val customerId: Long,
    val recipientEmail: String,
    val type: NotificationType,
    val channel: NotificationChannel,
    val status: NotificationStatus,
    val subject: String,
    val body: String,
    val referenceId: Long?,
    val createdAt: Instant?,
    val sentAt: Instant?,
)
```

Criar `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationPage.kt`:

```kotlin
package com.nexus.shopping.notification.domain

data class NotificationPage(
    val content: List<Notification>,
    val page: Int,
    val size: Int,
    val count: Int,
    val hasNext: Boolean,
)
```

- [ ] **Step 4: Criar o message renderer**

Criar `src/main/kotlin/com/nexus/shopping/notification/domain/NotificationMessageRenderer.kt`:

```kotlin
package com.nexus.shopping.notification.domain

data class RenderedMessage(
    val subject: String,
    val body: String,
)

object NotificationMessageRenderer {
    private data class MessageTemplate(
        val subject: String,
        val body: String,
    )

    private val placeholderPattern = Regex("\\{(\\w+)\\}")

    private val templates =
        mapOf(
            NotificationType.ORDER_CONFIRMED to
                MessageTemplate(
                    subject = "Pedido {orderId} confirmado",
                    body = "Seu pedido {orderId} no valor de {amount} foi confirmado.",
                ),
            NotificationType.ORDER_PAYMENT_FAILED to
                MessageTemplate(
                    subject = "Falha no pagamento do pedido {orderId}",
                    body = "O pagamento do pedido {orderId} no valor de {amount} falhou.",
                ),
            NotificationType.ORDER_CANCELLED to
                MessageTemplate(
                    subject = "Pedido {orderId} cancelado",
                    body = "Seu pedido {orderId} foi cancelado.",
                ),
        )

    fun requiredPlaceholders(type: NotificationType): Set<String> {
        val template = templates.getValue(type)
        return placeholderPattern
            .findAll("${template.subject} ${template.body}")
            .map { it.groupValues[1] }
            .toSet()
    }

    fun render(
        type: NotificationType,
        params: Map<String, String>,
    ): RenderedMessage {
        val template = templates.getValue(type)
        return RenderedMessage(
            subject = interpolate(template.subject, params),
            body = interpolate(template.body, params),
        )
    }

    private fun interpolate(
        template: String,
        params: Map<String, String>,
    ): String =
        placeholderPattern.replace(template) { match ->
            params[match.groupValues[1]] ?: match.value
        }
}
```

- [ ] **Step 5: Rodar o teste para confirmar sucesso**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.domain.NotificationMessageRendererTest"`
Expected: `BUILD SUCCESSFUL`, 4 testes passando.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/notification/domain/NotificationType.kt src/main/kotlin/com/nexus/shopping/notification/domain/NotificationChannel.kt src/main/kotlin/com/nexus/shopping/notification/domain/NotificationStatus.kt src/main/kotlin/com/nexus/shopping/notification/domain/Notification.kt src/main/kotlin/com/nexus/shopping/notification/domain/NotificationPage.kt src/main/kotlin/com/nexus/shopping/notification/domain/NotificationMessageRenderer.kt src/test/kotlin/com/nexus/shopping/notification/domain/NotificationMessageRendererTest.kt
git commit -m "feat: add notification domain model and message renderer"
```

---

## Task 2: Excecoes de aplicacao e PackageStructureArchitectureTest

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/exception/NotificationNotFoundException.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/exception/NotificationValidationException.kt`
- Modify: `src/test/kotlin/com/nexus/shopping/PackageStructureArchitectureTest.kt`

**Interfaces:**
- Consumes: `com.nexus.shopping.platform.application.exception.{ValidationException, NotFoundException}`.
- Produces: `NotificationValidationException(message: String)`, `NotificationNotFoundException(message: String)`.

- [ ] **Step 1: Escrever o teste falhando (estender PackageStructureArchitectureTest)**

Modificar `src/test/kotlin/com/nexus/shopping/PackageStructureArchitectureTest.kt`: no teste `application exceptions use platform base exceptions`, adicionar as classes de notification, e criar um novo teste para os DTOs HTTP de notification. Arquivo final:

```kotlin
package com.nexus.shopping

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackageStructureArchitectureTest {
    @Test
    fun `application exceptions use platform base exceptions`() {
        val validationException = Class.forName("com.nexus.shopping.platform.application.exception.ValidationException")
        val notFoundException = Class.forName("com.nexus.shopping.platform.application.exception.NotFoundException")
        val productValidationException =
            Class.forName("com.nexus.shopping.product.application.exception.ProductValidationException")
        val productNotFoundException =
            Class.forName("com.nexus.shopping.product.application.exception.ProductNotFoundException")
        val customerValidationException =
            Class.forName("com.nexus.shopping.customer.application.exception.CustomerValidationException")
        val customerNotFoundException =
            Class.forName("com.nexus.shopping.customer.application.exception.CustomerNotFoundException")
        val notificationValidationException =
            Class.forName("com.nexus.shopping.notification.application.exception.NotificationValidationException")
        val notificationNotFoundException =
            Class.forName("com.nexus.shopping.notification.application.exception.NotificationNotFoundException")

        assertTrue(validationException.isAssignableFrom(productValidationException))
        assertTrue(notFoundException.isAssignableFrom(productNotFoundException))
        assertTrue(validationException.isAssignableFrom(customerValidationException))
        assertTrue(notFoundException.isAssignableFrom(customerNotFoundException))
        assertTrue(validationException.isAssignableFrom(notificationValidationException))
        assertTrue(notFoundException.isAssignableFrom(notificationNotFoundException))
    }

    @Test
    fun `http exception handler is platform wide and does not import product classes`() {
        val handler = Class.forName("com.nexus.shopping.platform.adapter.inbound.http.ApiExceptionHandler")
        assertTrue(handler.simpleName == "ApiExceptionHandler")

        val handlerSource =
            java.nio.file.Path
                .of(
                    "src/main/kotlin/com/nexus/shopping/platform/adapter/inbound/http/ApiExceptionHandler.kt",
                ).toFile()
                .readText()

        assertFalse(handlerSource.contains("com.nexus.shopping.product"))
        assertFalse(handlerSource.contains("com.nexus.shopping.customer"))
        assertFalse(handlerSource.contains("com.nexus.shopping.notification"))
    }

    @Test
    fun `product http dto responses exist outside the domain package`() {
        Class.forName("com.nexus.shopping.product.adapter.inbound.http.dto.ProductResponse")
        Class.forName("com.nexus.shopping.product.adapter.inbound.http.dto.ProductPageResponse")
    }

    @Test
    fun `customer http dto responses exist outside the domain package`() {
        Class.forName("com.nexus.shopping.customer.adapter.inbound.http.dto.CustomerResponse")
    }

    @Test
    fun `codebase does not use a shared package for cross cutting structure`() {
        val sourceRoots =
            listOf(
                java.nio.file.Path
                    .of("src/main/kotlin/com/nexus/shopping"),
                java.nio.file.Path
                    .of("src/test/kotlin/com/nexus/shopping"),
            )
        val forbiddenPackage = "com.nexus.shopping." + "shared"
        val sharedPackageExists =
            sourceRoots.any { sourceRoot ->
                java.nio.file.Files.walk(sourceRoot).use { paths ->
                    paths
                        .filter { path ->
                            java.nio.file.Files
                                .isRegularFile(path) &&
                                path.toString().endsWith(".kt")
                        }.anyMatch { path ->
                            val source = path.toFile().readText()
                            path.toString().contains("/shared/") ||
                                source.lineSequence().any { line ->
                                    line.startsWith("package $forbiddenPackage") ||
                                        line.startsWith("import $forbiddenPackage")
                                }
                        }
                }
            }

        assertFalse(sharedPackageExists)
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar a falha**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.PackageStructureArchitectureTest"`
Expected: FAIL com `java.lang.ClassNotFoundException: com.nexus.shopping.notification.application.exception.NotificationValidationException` (ou compilacao falhando por classes ausentes).

- [ ] **Step 3: Criar as excecoes**

Criar `src/main/kotlin/com/nexus/shopping/notification/application/exception/NotificationNotFoundException.kt`:

```kotlin
package com.nexus.shopping.notification.application.exception

import com.nexus.shopping.platform.application.exception.NotFoundException

class NotificationNotFoundException(
    message: String,
) : NotFoundException(message)
```

Criar `src/main/kotlin/com/nexus/shopping/notification/application/exception/NotificationValidationException.kt`:

```kotlin
package com.nexus.shopping.notification.application.exception

import com.nexus.shopping.platform.application.exception.ValidationException

class NotificationValidationException(
    message: String,
) : ValidationException(message)
```

- [ ] **Step 4: Rodar o teste para confirmar sucesso**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.PackageStructureArchitectureTest"`
Expected: `BUILD SUCCESSFUL` para toda a classe `PackageStructureArchitectureTest` (o teste dos DTOs HTTP de notification sera adicionado apenas na Task 10, quando `NotificationResponse`/`NotificationPageResponse` existirem).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/notification/application/exception/NotificationNotFoundException.kt src/main/kotlin/com/nexus/shopping/notification/application/exception/NotificationValidationException.kt src/test/kotlin/com/nexus/shopping/PackageStructureArchitectureTest.kt
git commit -m "feat: add notification application exceptions"
```

---

## Task 3: Ports, command e logging helper

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/command/SendNotificationCommand.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/port/outbound/NotificationRepositoryPort.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/port/outbound/EmailSenderPort.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/usecase/NotificationUseCaseLogging.kt`

**Interfaces:**
- Consumes: `Notification`, `NotificationPage` (Task 1).
- Produces:
  - `data class SendNotificationCommand(customerId: Long, recipientEmail: String, type: String, referenceId: Long?, templateParams: Map<String, String>)`
  - `interface NotificationRepositoryPort { fun save(notification: Notification): Notification; fun findById(id: Long): Notification?; fun findByCustomerId(customerId: Long, page: Int, size: Int): NotificationPage }`
  - `data class EmailSendResult(success: Boolean, failureReason: String? = null)`
  - `interface EmailSenderPort { fun send(to: String, subject: String, body: String): EmailSendResult }`
  - `internal fun Logger.infoWithContext(message: String, vararg context: Pair<String, Any?>)`, `internal fun Logger.warnWithContext(...)` (uso interno do pacote `notification.application.usecase`).

Este task nao tem teste proprio (apenas tipos e interfaces sem logica); a validacao acontece indiretamente na Task 4, cujo teste falha ate esses tipos existirem.

- [ ] **Step 1: Criar o command**

Criar `src/main/kotlin/com/nexus/shopping/notification/application/command/SendNotificationCommand.kt`:

```kotlin
package com.nexus.shopping.notification.application.command

data class SendNotificationCommand(
    val customerId: Long,
    val recipientEmail: String,
    val type: String,
    val referenceId: Long?,
    val templateParams: Map<String, String>,
)
```

- [ ] **Step 2: Criar os ports**

Criar `src/main/kotlin/com/nexus/shopping/notification/application/port/outbound/NotificationRepositoryPort.kt`:

```kotlin
package com.nexus.shopping.notification.application.port.outbound

import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationPage

interface NotificationRepositoryPort {
    fun save(notification: Notification): Notification

    fun findById(id: Long): Notification?

    fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): NotificationPage
}
```

Criar `src/main/kotlin/com/nexus/shopping/notification/application/port/outbound/EmailSenderPort.kt`:

```kotlin
package com.nexus.shopping.notification.application.port.outbound

data class EmailSendResult(
    val success: Boolean,
    val failureReason: String? = null,
)

interface EmailSenderPort {
    fun send(
        to: String,
        subject: String,
        body: String,
    ): EmailSendResult
}
```

- [ ] **Step 3: Criar o logging helper**

Criar `src/main/kotlin/com/nexus/shopping/notification/application/usecase/NotificationUseCaseLogging.kt`:

```kotlin
package com.nexus.shopping.notification.application.usecase

import org.slf4j.Logger
import org.slf4j.MDC

internal fun Logger.infoWithContext(
    message: String,
    vararg context: Pair<String, Any?>,
) {
    logWithContext(context) {
        info(message)
    }
}

internal fun Logger.warnWithContext(
    message: String,
    vararg context: Pair<String, Any?>,
) {
    logWithContext(context) {
        warn(message)
    }
}

private fun logWithContext(
    context: Array<out Pair<String, Any?>>,
    log: () -> Unit,
) {
    try {
        context.forEach { (key, value) ->
            if (value != null) {
                MDC.put(key, value.toString())
            }
        }
        log()
    } finally {
        context.forEach { (key, _) ->
            MDC.remove(key)
        }
    }
}
```

- [ ] **Step 4: Verificar compilacao**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew compileKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/notification/application/command/SendNotificationCommand.kt src/main/kotlin/com/nexus/shopping/notification/application/port/outbound/NotificationRepositoryPort.kt src/main/kotlin/com/nexus/shopping/notification/application/port/outbound/EmailSenderPort.kt src/main/kotlin/com/nexus/shopping/notification/application/usecase/NotificationUseCaseLogging.kt
git commit -m "feat: add notification ports, command and logging helper"
```

---

## Task 4: SendNotificationUseCase

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/usecase/SendNotificationUseCase.kt`
- Test: `src/test/kotlin/com/nexus/shopping/notification/SendNotificationUseCaseTest.kt`

**Interfaces:**
- Consumes: `SendNotificationCommand`, `NotificationRepositoryPort`, `EmailSenderPort`, `EmailSendResult`, `Notification`, `NotificationType`, `NotificationChannel`, `NotificationStatus`, `NotificationMessageRenderer`, `NotificationValidationException` (Tasks 1-3).
- Produces: `class SendNotificationUseCase(notificationRepository: NotificationRepositoryPort, emailSender: EmailSenderPort) { fun send(command: SendNotificationCommand): Notification }`.

- [ ] **Step 1: Escrever o teste falhando**

Criar `src/test/kotlin/com/nexus/shopping/notification/SendNotificationUseCaseTest.kt`:

```kotlin
package com.nexus.shopping.notification

import com.nexus.shopping.notification.application.command.SendNotificationCommand
import com.nexus.shopping.notification.application.exception.NotificationValidationException
import com.nexus.shopping.notification.application.port.outbound.EmailSendResult
import com.nexus.shopping.notification.application.port.outbound.EmailSenderPort
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.application.usecase.SendNotificationUseCase
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationPage
import com.nexus.shopping.notification.domain.NotificationStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

private class FakeNotificationRepository : NotificationRepositoryPort {
    val saved = mutableListOf<Notification>()
    private var nextId = 1L

    override fun save(notification: Notification): Notification {
        val persisted = notification.copy(id = nextId++, createdAt = Instant.parse("2026-07-17T12:00:00Z"))
        saved += persisted
        return persisted
    }

    override fun findById(id: Long): Notification? = saved.find { it.id == id }

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): NotificationPage = throw UnsupportedOperationException()
}

private class FakeEmailSender(
    private val result: EmailSendResult = EmailSendResult(success = true),
) : EmailSenderPort {
    var lastTo: String? = null
    var lastSubject: String? = null
    var lastBody: String? = null

    override fun send(
        to: String,
        subject: String,
        body: String,
    ): EmailSendResult {
        lastTo = to
        lastSubject = subject
        lastBody = body
        return result
    }
}

class SendNotificationUseCaseTest {
    private fun validCommand() =
        SendNotificationCommand(
            customerId = 1L,
            recipientEmail = "cliente@example.com",
            type = "ORDER_CONFIRMED",
            referenceId = 123L,
            templateParams = mapOf("orderId" to "123", "amount" to "99.90"),
        )

    @Test
    fun `sends notification and persists it as SENT`() {
        val repository = FakeNotificationRepository()
        val emailSender = FakeEmailSender()
        val useCase = SendNotificationUseCase(repository, emailSender)

        val notification = useCase.send(validCommand())

        assertNotNull(notification.id)
        assertEquals(NotificationStatus.SENT, notification.status)
        assertEquals("Pedido 123 confirmado", notification.subject)
        assertEquals("Seu pedido 123 no valor de 99.90 foi confirmado.", notification.body)
        assertEquals("cliente@example.com", emailSender.lastTo)
        assertNotNull(notification.sentAt)
    }

    @Test
    fun `persists notification as FAILED when email sender reports failure`() {
        val repository = FakeNotificationRepository()
        val emailSender = FakeEmailSender(EmailSendResult(success = false, failureReason = "smtp down"))
        val useCase = SendNotificationUseCase(repository, emailSender)

        val notification = useCase.send(validCommand())

        assertEquals(NotificationStatus.FAILED, notification.status)
        assertEquals(null, notification.sentAt)
    }

    @Test
    fun `throws NotificationValidationException when customerId is not positive`() {
        val useCase = SendNotificationUseCase(FakeNotificationRepository(), FakeEmailSender())

        assertFailsWith<NotificationValidationException> {
            useCase.send(validCommand().copy(customerId = 0L))
        }
    }

    @Test
    fun `throws NotificationValidationException when recipientEmail is invalid`() {
        val useCase = SendNotificationUseCase(FakeNotificationRepository(), FakeEmailSender())

        assertFailsWith<NotificationValidationException> {
            useCase.send(validCommand().copy(recipientEmail = "invalid-email"))
        }
    }

    @Test
    fun `throws NotificationValidationException when type is invalid`() {
        val useCase = SendNotificationUseCase(FakeNotificationRepository(), FakeEmailSender())

        assertFailsWith<NotificationValidationException> {
            useCase.send(validCommand().copy(type = "ORDER_SHIPPED"))
        }
    }

    @Test
    fun `throws NotificationValidationException listing missing template params`() {
        val useCase = SendNotificationUseCase(FakeNotificationRepository(), FakeEmailSender())

        val exception =
            assertFailsWith<NotificationValidationException> {
                useCase.send(validCommand().copy(templateParams = mapOf("orderId" to "123")))
            }
        assertEquals(true, exception.message?.contains("amount"))
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar a falha**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.SendNotificationUseCaseTest"`
Expected: FAIL com `Unresolved reference: SendNotificationUseCase`.

- [ ] **Step 3: Implementar o use case**

Criar `src/main/kotlin/com/nexus/shopping/notification/application/usecase/SendNotificationUseCase.kt`:

```kotlin
package com.nexus.shopping.notification.application.usecase

import com.nexus.shopping.notification.application.command.SendNotificationCommand
import com.nexus.shopping.notification.application.exception.NotificationValidationException
import com.nexus.shopping.notification.application.port.outbound.EmailSenderPort
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationChannel
import com.nexus.shopping.notification.domain.NotificationMessageRenderer
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.notification.domain.NotificationType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SendNotificationUseCase(
    private val notificationRepository: NotificationRepositoryPort,
    private val emailSender: EmailSenderPort,
) {
    fun send(command: SendNotificationCommand): Notification {
        logger.infoWithContext(
            "notification.send.started",
            "notification.customer_id" to command.customerId,
            "notification.type" to command.type,
        )

        if (command.customerId <= 0) throwValidationFailed("customerId must be greater than 0.")
        if (command.recipientEmail.isBlank()) throwValidationFailed("recipientEmail must not be blank.")
        if (command.recipientEmail.length > 254) throwValidationFailed("recipientEmail must be at most 254 characters.")
        if (!command.recipientEmail.contains("@")) throwValidationFailed("recipientEmail must be valid.")

        val type = requireValidType(command.type)

        val missingParams = NotificationMessageRenderer.requiredPlaceholders(type) - command.templateParams.keys
        if (missingParams.isNotEmpty()) {
            throwValidationFailed(
                "Missing required templateParams for type ${type.name}: ${missingParams.sorted().joinToString(", ")}.",
            )
        }

        val message = NotificationMessageRenderer.render(type, command.templateParams)
        val result = emailSender.send(command.recipientEmail, message.subject, message.body)

        val notification =
            Notification(
                id = null,
                customerId = command.customerId,
                recipientEmail = command.recipientEmail,
                type = type,
                channel = NotificationChannel.EMAIL,
                status = if (result.success) NotificationStatus.SENT else NotificationStatus.FAILED,
                subject = message.subject,
                body = message.body,
                referenceId = command.referenceId,
                createdAt = null,
                sentAt = if (result.success) Instant.now() else null,
            )

        val saved = notificationRepository.save(notification)
        logger.infoWithContext(
            "notification.send.completed",
            "notification.id" to saved.id,
            "notification.status" to saved.status,
        )
        return saved
    }

    private fun requireValidType(type: String): NotificationType {
        val validNames = NotificationType.entries.map { it.name }
        if (type !in validNames) {
            throwValidationFailed("type must be one of: ${validNames.joinToString(", ")}.")
        }
        return NotificationType.valueOf(type)
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("notification.send.validation_failed", "validation.error" to message)
        throw NotificationValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SendNotificationUseCase::class.java)
    }
}
```

- [ ] **Step 4: Rodar o teste para confirmar sucesso**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.SendNotificationUseCaseTest"`
Expected: `BUILD SUCCESSFUL`, 6 testes passando.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/notification/application/usecase/SendNotificationUseCase.kt src/test/kotlin/com/nexus/shopping/notification/SendNotificationUseCaseTest.kt
git commit -m "feat: add SendNotificationUseCase with template param validation"
```

---

## Task 5: GetNotificationByIdUseCase

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/usecase/GetNotificationByIdUseCase.kt`
- Test: `src/test/kotlin/com/nexus/shopping/notification/GetNotificationByIdUseCaseTest.kt`

**Interfaces:**
- Consumes: `NotificationRepositoryPort`, `Notification`, `NotificationNotFoundException` (Tasks 1-3).
- Produces: `class GetNotificationByIdUseCase(notificationRepository: NotificationRepositoryPort) { fun execute(id: Long): Notification }`.

- [ ] **Step 1: Escrever o teste falhando**

Criar `src/test/kotlin/com/nexus/shopping/notification/GetNotificationByIdUseCaseTest.kt`:

```kotlin
package com.nexus.shopping.notification

import com.nexus.shopping.notification.application.exception.NotificationNotFoundException
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.application.usecase.GetNotificationByIdUseCase
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationChannel
import com.nexus.shopping.notification.domain.NotificationPage
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.notification.domain.NotificationType
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeNotificationRepository(
    private val notifications: Map<Long, Notification>,
) : NotificationRepositoryPort {
    override fun save(notification: Notification): Notification = throw UnsupportedOperationException()

    override fun findById(id: Long): Notification? = notifications[id]

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): NotificationPage = throw UnsupportedOperationException()
}

class GetNotificationByIdUseCaseTest {
    private fun sampleNotification(id: Long) =
        Notification(
            id = id,
            customerId = 1L,
            recipientEmail = "cliente@example.com",
            type = NotificationType.ORDER_CONFIRMED,
            channel = NotificationChannel.EMAIL,
            status = NotificationStatus.SENT,
            subject = "Pedido 123 confirmado",
            body = "Seu pedido 123 no valor de 99.90 foi confirmado.",
            referenceId = 123L,
            createdAt = Instant.parse("2026-07-17T12:00:00Z"),
            sentAt = Instant.parse("2026-07-17T12:00:00Z"),
        )

    @Test
    fun `returns notification when found`() {
        val useCase = GetNotificationByIdUseCase(FakeNotificationRepository(mapOf(1L to sampleNotification(1L))))

        val notification = useCase.execute(1L)

        assertEquals(1L, notification.id)
        assertEquals("Pedido 123 confirmado", notification.subject)
    }

    @Test
    fun `throws NotificationNotFoundException when not found`() {
        val useCase = GetNotificationByIdUseCase(FakeNotificationRepository(emptyMap()))

        assertFailsWith<NotificationNotFoundException> {
            useCase.execute(9999L)
        }
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar a falha**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.GetNotificationByIdUseCaseTest"`
Expected: FAIL com `Unresolved reference: GetNotificationByIdUseCase`.

- [ ] **Step 3: Implementar o use case**

Criar `src/main/kotlin/com/nexus/shopping/notification/application/usecase/GetNotificationByIdUseCase.kt`:

```kotlin
package com.nexus.shopping.notification.application.usecase

import com.nexus.shopping.notification.application.exception.NotificationNotFoundException
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.domain.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GetNotificationByIdUseCase(
    private val notificationRepository: NotificationRepositoryPort,
) {
    fun execute(id: Long): Notification {
        logger.infoWithContext("notification.get_by_id.started", "notification.id" to id)

        val notification = notificationRepository.findById(id)
        if (notification == null) {
            logger.warnWithContext("notification.get_by_id.not_found", "notification.id" to id)
            throw NotificationNotFoundException("Notification $id not found.")
        }

        logger.infoWithContext("notification.get_by_id.completed", "notification.id" to notification.id)
        return notification
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(GetNotificationByIdUseCase::class.java)
    }
}
```

- [ ] **Step 4: Rodar o teste para confirmar sucesso**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.GetNotificationByIdUseCaseTest"`
Expected: `BUILD SUCCESSFUL`, 2 testes passando.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/notification/application/usecase/GetNotificationByIdUseCase.kt src/test/kotlin/com/nexus/shopping/notification/GetNotificationByIdUseCaseTest.kt
git commit -m "feat: add GetNotificationByIdUseCase"
```

---

## Task 6: ListNotificationsByCustomerUseCase

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/notification/application/usecase/ListNotificationsByCustomerUseCase.kt`
- Test: `src/test/kotlin/com/nexus/shopping/notification/ListNotificationsByCustomerUseCaseTest.kt`

**Interfaces:**
- Consumes: `NotificationRepositoryPort`, `NotificationPage`, `NotificationValidationException` (Tasks 1-3).
- Produces: `class ListNotificationsByCustomerUseCase(notificationRepository: NotificationRepositoryPort) { fun list(customerId: Long?, page: Int, size: Int): NotificationPage }`.

- [ ] **Step 1: Escrever o teste falhando**

Criar `src/test/kotlin/com/nexus/shopping/notification/ListNotificationsByCustomerUseCaseTest.kt`:

```kotlin
package com.nexus.shopping.notification

import com.nexus.shopping.notification.application.exception.NotificationValidationException
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.application.usecase.ListNotificationsByCustomerUseCase
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeNotificationRepository : NotificationRepositoryPort {
    var lastCustomerId: Long? = null
    var lastPage: Int? = null
    var lastSize: Int? = null

    override fun save(notification: Notification): Notification = throw UnsupportedOperationException()

    override fun findById(id: Long): Notification? = throw UnsupportedOperationException()

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): NotificationPage {
        lastCustomerId = customerId
        lastPage = page
        lastSize = size
        return NotificationPage(content = emptyList(), page = page, size = size, count = 0, hasNext = false)
    }
}

class ListNotificationsByCustomerUseCaseTest {
    @Test
    fun `delegates to repository with given customerId, page and size`() {
        val repository = FakeNotificationRepository()
        val useCase = ListNotificationsByCustomerUseCase(repository)

        val result = useCase.list(customerId = 1L, page = 0, size = 50)

        assertEquals(1L, repository.lastCustomerId)
        assertEquals(0, repository.lastPage)
        assertEquals(50, repository.lastSize)
        assertEquals(0, result.count)
    }

    @Test
    fun `throws NotificationValidationException when customerId is missing`() {
        val useCase = ListNotificationsByCustomerUseCase(FakeNotificationRepository())

        assertFailsWith<NotificationValidationException> {
            useCase.list(customerId = null, page = 0, size = 50)
        }
    }

    @Test
    fun `throws NotificationValidationException when page is negative`() {
        val useCase = ListNotificationsByCustomerUseCase(FakeNotificationRepository())

        assertFailsWith<NotificationValidationException> {
            useCase.list(customerId = 1L, page = -1, size = 50)
        }
    }

    @Test
    fun `throws NotificationValidationException when size is out of range`() {
        val useCase = ListNotificationsByCustomerUseCase(FakeNotificationRepository())

        assertFailsWith<NotificationValidationException> {
            useCase.list(customerId = 1L, page = 0, size = 501)
        }
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar a falha**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.ListNotificationsByCustomerUseCaseTest"`
Expected: FAIL com `Unresolved reference: ListNotificationsByCustomerUseCase`.

- [ ] **Step 3: Implementar o use case**

Criar `src/main/kotlin/com/nexus/shopping/notification/application/usecase/ListNotificationsByCustomerUseCase.kt`:

```kotlin
package com.nexus.shopping.notification.application.usecase

import com.nexus.shopping.notification.application.exception.NotificationValidationException
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.domain.NotificationPage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ListNotificationsByCustomerUseCase(
    private val notificationRepository: NotificationRepositoryPort,
) {
    fun list(
        customerId: Long?,
        page: Int,
        size: Int,
    ): NotificationPage {
        logger.infoWithContext(
            "notification.list_by_customer.started",
            "notification.customer_id" to customerId,
            "notification.page" to page,
            "notification.size" to size,
        )

        if (customerId == null) throwValidationFailed("Query parameter customerId is required.")
        if (page < 0) throwValidationFailed("Query parameter page must be greater than or equal to 0.")
        if (size !in 1..500) throwValidationFailed("Query parameter size must be between 1 and 500.")

        val result = notificationRepository.findByCustomerId(customerId, page, size)

        logger.infoWithContext(
            "notification.list_by_customer.completed",
            "notification.customer_id" to customerId,
            "notification.count" to result.count,
            "notification.has_next" to result.hasNext,
        )
        return result
    }

    private fun throwValidationFailed(message: String): Nothing {
        logger.warnWithContext("notification.list_by_customer.validation_failed", "validation.error" to message)
        throw NotificationValidationException(message)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ListNotificationsByCustomerUseCase::class.java)
    }
}
```

- [ ] **Step 4: Rodar o teste para confirmar sucesso**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.ListNotificationsByCustomerUseCaseTest"`
Expected: `BUILD SUCCESSFUL`, 4 testes passando.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/notification/application/usecase/ListNotificationsByCustomerUseCase.kt src/test/kotlin/com/nexus/shopping/notification/ListNotificationsByCustomerUseCaseTest.kt
git commit -m "feat: add ListNotificationsByCustomerUseCase"
```

---

## Task 7: Migration V6 e teste de contrato

**Files:**
- Create: `src/main/resources/db/migration/V6__create_notification_context.sql`
- Test: `src/test/kotlin/com/nexus/shopping/notification/NotificationMigrationContractTest.kt`

**Interfaces:**
- Consumes: migrations existentes `V1`-`V5` (tabela `customers`, id=1 sempre seedado como "Benjamin Bryan Duarte").
- Produces: tabela `notifications` com FK `fk_notifications_customer` e index `idx_notifications_customer_id`.

- [ ] **Step 1: Escrever o teste falhando**

Criar `src/test/kotlin/com/nexus/shopping/notification/NotificationMigrationContractTest.kt`:

```kotlin
package com.nexus.shopping.notification

import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NotificationMigrationContractTest {
    @Test
    fun `notifications table has customer FK, indexes and accepts a valid row`() {
        val jdbcUrl = "jdbc:h2:mem:notification_migration_contract;DB_CLOSE_DELAY=-1"
        Flyway
            .configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations("classpath:db/migration")
            .placeholders(mapOf("productSeedCount" to "10"))
            .load()
            .migrate()

        DriverManager.getConnection(jdbcUrl, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    INSERT INTO notifications
                        (customer_id, recipient_email, type, channel, status, subject, body, reference_id)
                    VALUES
                        (1, 'benjamin-duarte86@lexos.com.br', 'ORDER_CONFIRMED', 'EMAIL', 'SENT',
                         'Pedido 123 confirmado', 'Seu pedido 123 no valor de 99.90 foi confirmado.', 123)
                    """.trimIndent(),
                )
            }
            assertEquals(1, countRows(connection, "notifications"))
            assertEquals(1, countRows(connection, "notifications WHERE customer_id = 1 AND status = 'SENT'"))

            assertFailsWith<SQLException> {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        INSERT INTO notifications
                            (customer_id, recipient_email, type, channel, status, subject, body)
                        VALUES
                            (999999, 'ghost@example.com', 'ORDER_CONFIRMED', 'EMAIL', 'SENT', 'x', 'y')
                        """.trimIndent(),
                    )
                }
            }

            assertTrue(
                countRows(
                    connection,
                    "INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'NOTIFICATIONS' AND INDEX_NAME = 'IDX_NOTIFICATIONS_CUSTOMER_ID'",
                ) >= 1,
            )
            assertTrue(
                countRows(
                    connection,
                    "INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                        "WHERE TABLE_NAME = 'NOTIFICATIONS' AND CONSTRAINT_NAME = 'FK_NOTIFICATIONS_CUSTOMER'",
                ) >= 1,
            )
        }
    }

    private fun countRows(
        connection: Connection,
        fromClause: String,
    ): Int {
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM $fromClause").use { resultSet ->
                resultSet.next()
                return resultSet.getInt(1)
            }
        }
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar a falha**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.NotificationMigrationContractTest"`
Expected: FAIL com `org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "NOTIFICATIONS" not found`.

- [ ] **Step 3: Criar a migration V6 (conteudo verbatim da spec aprovada)**

Criar `src/main/resources/db/migration/V6__create_notification_context.sql`:

```sql
CREATE TABLE notifications (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    recipient_email VARCHAR(254) NOT NULL,
    type VARCHAR(32) NOT NULL,
    channel VARCHAR(16) NOT NULL DEFAULT 'EMAIL',
    status VARCHAR(16) NOT NULL,
    subject VARCHAR(180) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    reference_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    CONSTRAINT notifications_type_check CHECK (type IN ('ORDER_CONFIRMED', 'ORDER_PAYMENT_FAILED', 'ORDER_CANCELLED')),
    CONSTRAINT notifications_channel_check CHECK (channel IN ('EMAIL')),
    CONSTRAINT notifications_status_check CHECK (status IN ('SENT', 'FAILED')),
    CONSTRAINT fk_notifications_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE INDEX idx_notifications_customer_id ON notifications (customer_id);
```

- [ ] **Step 4: Rodar o teste para confirmar sucesso**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.NotificationMigrationContractTest"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V6__create_notification_context.sql src/test/kotlin/com/nexus/shopping/notification/NotificationMigrationContractTest.kt
git commit -m "feat: add notifications table migration with customer FK"
```

---

## Task 8: Adapter JPA (entity, Spring Data repository, adapter)

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/NotificationEntity.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/SpringDataNotificationRepository.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/NotificationJpaRepositoryAdapter.kt`
- Test: `src/test/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/NotificationEntityTest.kt`

**Interfaces:**
- Consumes: `Notification`, `NotificationType`, `NotificationChannel`, `NotificationStatus`, `NotificationPage`, `NotificationRepositoryPort` (Tasks 1, 3), tabela `notifications` (Task 7).
- Produces: `class NotificationJpaRepositoryAdapter(repository: SpringDataNotificationRepository) : NotificationRepositoryPort`, `fun NotificationEntity.toDomain(): Notification`, `fun Notification.toEntity(): NotificationEntity`.

- [ ] **Step 1: Escrever o teste falhando (toDomain requer todos os campos)**

Criar `src/test/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/NotificationEntityTest.kt`:

```kotlin
package com.nexus.shopping.notification.adapter.outbound.jpa

import com.nexus.shopping.notification.domain.NotificationChannel
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.notification.domain.NotificationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NotificationEntityTest {
    @Test
    fun `toDomain maps all fields`() {
        val entity =
            NotificationEntity(
                id = 10L,
                customerId = 1L,
                recipientEmail = "cliente@example.com",
                type = NotificationType.ORDER_CONFIRMED,
                channel = NotificationChannel.EMAIL,
                status = NotificationStatus.SENT,
                subject = "Pedido 123 confirmado",
                body = "Seu pedido 123 no valor de 99.90 foi confirmado.",
                referenceId = 123L,
                createdAt = java.time.Instant.parse("2026-07-17T12:00:00Z"),
                sentAt = java.time.Instant.parse("2026-07-17T12:00:00Z"),
            )

        val notification = entity.toDomain()

        assertEquals(10L, notification.id)
        assertEquals(1L, notification.customerId)
        assertEquals(NotificationStatus.SENT, notification.status)
        assertEquals(123L, notification.referenceId)
    }

    @Test
    fun `toDomain throws when id is missing`() {
        val entity =
            NotificationEntity(
                id = null,
                customerId = 1L,
                recipientEmail = "cliente@example.com",
                type = NotificationType.ORDER_CONFIRMED,
                channel = NotificationChannel.EMAIL,
                status = NotificationStatus.SENT,
                subject = "Pedido 123 confirmado",
                body = "Seu pedido 123 no valor de 99.90 foi confirmado.",
                referenceId = null,
                createdAt = java.time.Instant.parse("2026-07-17T12:00:00Z"),
                sentAt = null,
            )

        assertFailsWith<IllegalArgumentException> {
            entity.toDomain()
        }
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar a falha**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.adapter.outbound.jpa.NotificationEntityTest"`
Expected: FAIL com `Unresolved reference: NotificationEntity`.

- [ ] **Step 3: Criar a entidade JPA**

Criar `src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/NotificationEntity.kt`:

```kotlin
package com.nexus.shopping.notification.adapter.outbound.jpa

import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationChannel
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.notification.domain.NotificationType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SourceType
import java.time.Instant

@Entity
@Table(name = "notifications")
class NotificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "customer_id", nullable = false)
    var customerId: Long = 0,
    @Column(name = "recipient_email", nullable = false, length = 254)
    var recipientEmail: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    var type: NotificationType = NotificationType.ORDER_CONFIRMED,
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    var channel: NotificationChannel = NotificationChannel.EMAIL,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: NotificationStatus = NotificationStatus.SENT,
    @Column(name = "subject", nullable = false, length = 180)
    var subject: String = "",
    @Column(name = "body", nullable = false, length = 2000)
    var body: String = "",
    @Column(name = "reference_id")
    var referenceId: Long? = null,
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @Column(name = "sent_at")
    var sentAt: Instant? = null,
) {
    fun toDomain(): Notification =
        Notification(
            id = requireNotNull(id) { "NotificationEntity.id must be available before mapping to domain." },
            customerId = customerId,
            recipientEmail = recipientEmail,
            type = type,
            channel = channel,
            status = status,
            subject = subject,
            body = body,
            referenceId = referenceId,
            createdAt = requireNotNull(createdAt) { "NotificationEntity.createdAt must be available before mapping to domain." },
            sentAt = sentAt,
        )
}

fun Notification.toEntity(): NotificationEntity =
    NotificationEntity(
        customerId = customerId,
        recipientEmail = recipientEmail,
        type = type,
        channel = channel,
        status = status,
        subject = subject,
        body = body,
        referenceId = referenceId,
        sentAt = sentAt,
    )
```

- [ ] **Step 4: Rodar o teste da entidade para confirmar sucesso**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.adapter.outbound.jpa.NotificationEntityTest"`
Expected: `BUILD SUCCESSFUL`, 2 testes passando.

- [ ] **Step 5: Criar o repository Spring Data**

Criar `src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/SpringDataNotificationRepository.kt`:

```kotlin
package com.nexus.shopping.notification.adapter.outbound.jpa

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpringDataNotificationRepository : JpaRepository<NotificationEntity, Long> {
    @Query(
        """
        SELECT n FROM NotificationEntity n
        WHERE n.customerId = :customerId
        ORDER BY n.id
        """,
    )
    fun findByCustomerId(
        @Param("customerId") customerId: Long,
        pageable: Pageable,
    ): Slice<NotificationEntity>
}
```

- [ ] **Step 6: Criar o adapter**

Criar `src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/NotificationJpaRepositoryAdapter.kt`:

```kotlin
package com.nexus.shopping.notification.adapter.outbound.jpa

import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationPage
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class NotificationJpaRepositoryAdapter(
    private val repository: SpringDataNotificationRepository,
) : NotificationRepositoryPort {
    @Transactional
    override fun save(notification: Notification): Notification = repository.saveAndFlush(notification.toEntity()).toDomain()

    @Transactional(readOnly = true)
    override fun findById(id: Long): Notification? = repository.findById(id).orElse(null)?.toDomain()

    @Transactional(readOnly = true)
    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): NotificationPage {
        val slice = repository.findByCustomerId(customerId, PageRequest.of(page, size))
        val content = slice.content.map { it.toDomain() }

        return NotificationPage(
            content = content,
            page = page,
            size = size,
            count = content.size,
            hasNext = slice.hasNext(),
        )
    }
}
```

- [ ] **Step 7: Rodar todos os testes do modulo notification**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.*"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/NotificationEntity.kt src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/SpringDataNotificationRepository.kt src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/NotificationJpaRepositoryAdapter.kt src/test/kotlin/com/nexus/shopping/notification/adapter/outbound/jpa/NotificationEntityTest.kt
git commit -m "feat: add notification JPA adapter with slice pagination"
```

---

## Task 9: LoggingEmailSenderAdapter

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/email/LoggingEmailSenderAdapter.kt`
- Test: `src/test/kotlin/com/nexus/shopping/notification/adapter/outbound/email/LoggingEmailSenderAdapterTest.kt`
- Delete: `src/test/kotlin/com/nexus/shopping/notification/adapter/NotificationTestConfiguration.kt`
- Delete: `src/test/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**Interfaces:**
- Consumes: `EmailSenderPort`, `EmailSendResult` (Task 3).
- Produces: `class LoggingEmailSenderAdapter : EmailSenderPort { override fun send(to: String, subject: String, body: String): EmailSendResult }`.

- [ ] **Step 1: Escrever o teste falhando**

Criar `src/test/kotlin/com/nexus/shopping/notification/adapter/outbound/email/LoggingEmailSenderAdapterTest.kt`:

```kotlin
package com.nexus.shopping.notification.adapter.outbound.email

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoggingEmailSenderAdapterTest {
    @Test
    fun `send always reports success without failureReason`() {
        val adapter = LoggingEmailSenderAdapter()

        val result = adapter.send("cliente@example.com", "Pedido 123 confirmado", "Seu pedido foi confirmado.")

        assertTrue(result.success)
        assertNull(result.failureReason)
        assertEquals(true, result.success)
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar a falha**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.adapter.outbound.email.LoggingEmailSenderAdapterTest"`
Expected: FAIL com `Unresolved reference: LoggingEmailSenderAdapter`.

- [ ] **Step 3: Implementar o adapter**

Criar `src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/email/LoggingEmailSenderAdapter.kt`:

```kotlin
package com.nexus.shopping.notification.adapter.outbound.email

import com.nexus.shopping.notification.application.port.outbound.EmailSendResult
import com.nexus.shopping.notification.application.port.outbound.EmailSenderPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LoggingEmailSenderAdapter : EmailSenderPort {
    override fun send(
        to: String,
        subject: String,
        body: String,
    ): EmailSendResult {
        logger.info("Simulated email sent to {} subject={}", to, subject)
        return EmailSendResult(success = true)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(LoggingEmailSenderAdapter::class.java)
    }
}
```

- [ ] **Step 4: Rodar o teste para confirmar sucesso**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.adapter.outbound.email.LoggingEmailSenderAdapterTest"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Remover o stub de contexto (real beans agora existem para ambos os ports)**

Task 4 introduziu um stub temporario (`NotificationTestConfiguration.kt` + `AutoConfiguration.imports`) para manter verdes os testes `@SpringBootTest` existentes (`CustomerControllerTest`, `HealthEndpointTest`, `ProductControllerTest` etc.) enquanto `NotificationRepositoryPort` (Task 8: `NotificationJpaRepositoryAdapter`) e `EmailSenderPort` (esta task: `LoggingEmailSenderAdapter`) ainda nao tinham beans reais. Agora que ambos existem, o stub `@Primary` passa a ser um risco: ele continuaria sombreando os adapters reais em qualquer `@SpringBootTest` futuro (incluindo o `NotificationControllerTest` da Task 10), escondendo cobertura de integracao real. Remover:

```bash
git rm src/test/kotlin/com/nexus/shopping/notification/adapter/NotificationTestConfiguration.kt
git rm src/test/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build`
Expected: `BUILD SUCCESSFUL`, sem `NoSuchBeanDefinitionException` em nenhum contexto (confirma que os testes `@SpringBootTest` pre-existentes continuam verdes usando os beans reais).

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/notification/adapter/outbound/email/LoggingEmailSenderAdapter.kt src/test/kotlin/com/nexus/shopping/notification/adapter/outbound/email/LoggingEmailSenderAdapterTest.kt
git rm src/test/kotlin/com/nexus/shopping/notification/adapter/NotificationTestConfiguration.kt
git rm src/test/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
git commit -m "feat: add simulated LoggingEmailSenderAdapter and remove interim test stub for notification ports"
```

---

## Task 10: DTOs HTTP e NotificationController

**Files:**
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/dto/SendNotificationRequest.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/dto/NotificationResponse.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/dto/NotificationPageResponse.kt`
- Create: `src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/NotificationController.kt`
- Test: `src/test/kotlin/com/nexus/shopping/notification/NotificationControllerTest.kt`
- Modify: `src/test/kotlin/com/nexus/shopping/PackageStructureArchitectureTest.kt`

**Interfaces:**
- Consumes: `SendNotificationUseCase`, `GetNotificationByIdUseCase`, `ListNotificationsByCustomerUseCase`, `Notification`, `NotificationPage` (Tasks 1, 4, 5, 6).
- Produces:
  - `data class SendNotificationRequest(customerId: Long, recipientEmail: String, type: String, referenceId: Long? = null, templateParams: Map<String, String> = emptyMap())` + `fun SendNotificationRequest.toCommand(): SendNotificationCommand`
  - `data class NotificationResponse(id, customerId, recipientEmail, type, channel, status, subject, body, referenceId, createdAt: Instant, sentAt: Instant?)` + `fun Notification.toResponse(): NotificationResponse`
  - `data class NotificationPageResponse(content, page, size, count, hasNext)` + `fun NotificationPage.toResponse(): NotificationPageResponse`
  - `POST /notifications`, `GET /notifications/{id}`, `GET /notifications?customerId=&page=&size=`

- [ ] **Step 1: Escrever o teste falhando (SpringBootTest de ponta a ponta)**

Criar `src/test/kotlin/com/nexus/shopping/notification/NotificationControllerTest.kt`:

```kotlin
package com.nexus.shopping.notification

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:nexus_shopping_notification_controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.placeholders.productSeedCount=3",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
class NotificationControllerTest {
    @Autowired
    private lateinit var environment: Environment

    private val mapper = JsonMapper.builder().build()
    private val httpClient = HttpClient.newHttpClient()

    private fun post(
        port: String,
        body: String,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/notifications"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun get(
        port: String,
        path: String,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .GET()
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun assertExceptionDetail(
        response: HttpResponse<String>,
        expectedStatus: Int,
        expectedTitle: String,
        expectedInstance: String,
    ): JsonNode {
        assertEquals(expectedStatus, response.statusCode())
        assertTrue(
            response
                .headers()
                .firstValue("Content-Type")
                .orElse("")
                .startsWith("application/problem+json"),
        )
        val exceptionDetail = mapper.readTree(response.body())
        assertEquals("about:blank", exceptionDetail["type"].asText())
        assertEquals(expectedTitle, exceptionDetail["title"].asText())
        assertEquals(expectedStatus, exceptionDetail["status"].asInt())
        assertEquals(expectedInstance, exceptionDetail["instance"].asText())
        return exceptionDetail
    }

    @Test
    fun `POST notifications returns 201 with sent notification`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "customerId": 1,
              "recipientEmail": "cliente@example.com",
              "type": "ORDER_CONFIRMED",
              "referenceId": 123,
              "templateParams": { "orderId": "123", "amount": "99.90" }
            }
            """.trimIndent()

        val response = post(port, body)

        assertEquals(201, response.statusCode())
        val notification = mapper.readTree(response.body())
        assertNotNull(notification["id"].asLong().takeIf { it > 0 }, "Expected a generated id > 0")
        assertEquals(1L, notification["customerId"].asLong())
        assertEquals("cliente@example.com", notification["recipientEmail"].asText())
        assertEquals("ORDER_CONFIRMED", notification["type"].asText())
        assertEquals("EMAIL", notification["channel"].asText())
        assertEquals("SENT", notification["status"].asText())
        assertEquals("Pedido 123 confirmado", notification["subject"].asText())
        assertEquals("Seu pedido 123 no valor de 99.90 foi confirmado.", notification["body"].asText())
        assertEquals(123L, notification["referenceId"].asLong())
        assertNotNull(notification["sentAt"].asText())
    }

    @Test
    fun `POST notifications with missing template param returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "customerId": 1,
              "recipientEmail": "cliente@example.com",
              "type": "ORDER_CONFIRMED",
              "templateParams": { "orderId": "123" }
            }
            """.trimIndent()

        val response = post(port, body)

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/notifications",
        )
    }

    @Test
    fun `GET notification by id returns 200`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "customerId": 2,
              "recipientEmail": "outro@example.com",
              "type": "ORDER_CANCELLED",
              "templateParams": { "orderId": "456" }
            }
            """.trimIndent()
        val created = mapper.readTree(post(port, body).body())

        val response = get(port, "/notifications/${created["id"].asLong()}")

        assertEquals(200, response.statusCode())
        val notification = mapper.readTree(response.body())
        assertEquals(created["id"].asLong(), notification["id"].asLong())
        assertEquals("Pedido 456 cancelado", notification["subject"].asText())
    }

    @Test
    fun `GET notification by id with non-existent id returns 404 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "/notifications/9999999999")

        assertExceptionDetail(
            response = response,
            expectedStatus = 404,
            expectedTitle = "Not Found",
            expectedInstance = "/notifications/9999999999",
        )
    }

    @Test
    fun `GET notifications without customerId returns 400 problem details`() {
        val port = environment.getRequiredProperty("local.server.port")

        val response = get(port, "/notifications")

        assertExceptionDetail(
            response = response,
            expectedStatus = 400,
            expectedTitle = "Bad Request",
            expectedInstance = "/notifications",
        )
    }

    @Test
    fun `GET notifications by customerId returns paginated content`() {
        val port = environment.getRequiredProperty("local.server.port")
        val body =
            """
            {
              "customerId": 3,
              "recipientEmail": "terceiro@example.com",
              "type": "ORDER_PAYMENT_FAILED",
              "templateParams": { "orderId": "789", "amount": "10.00" }
            }
            """.trimIndent()
        post(port, body)

        val response = get(port, "/notifications?customerId=3&page=0&size=50")

        assertEquals(200, response.statusCode())
        val page = mapper.readTree(response.body())
        assertEquals(0, page["page"].asInt())
        assertEquals(50, page["size"].asInt())
        assertTrue(page["content"].isArray)
        assertTrue(page["content"].size() >= 1)
        assertEquals(false, page["hasNext"].asBoolean())
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar a falha**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.NotificationControllerTest"`
Expected: FAIL (404/connection refused ou erro de compilacao) pois `/notifications` ainda nao existe.

- [ ] **Step 3: Criar os DTOs**

Criar `src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/dto/SendNotificationRequest.kt`:

```kotlin
package com.nexus.shopping.notification.adapter.inbound.http.dto

import com.nexus.shopping.notification.application.command.SendNotificationCommand

data class SendNotificationRequest(
    val customerId: Long,
    val recipientEmail: String,
    val type: String,
    val referenceId: Long? = null,
    val templateParams: Map<String, String> = emptyMap(),
)

fun SendNotificationRequest.toCommand(): SendNotificationCommand =
    SendNotificationCommand(
        customerId = customerId,
        recipientEmail = recipientEmail,
        type = type,
        referenceId = referenceId,
        templateParams = templateParams,
    )
```

Criar `src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/dto/NotificationResponse.kt`:

```kotlin
package com.nexus.shopping.notification.adapter.inbound.http.dto

import com.nexus.shopping.notification.domain.Notification
import java.time.Instant

data class NotificationResponse(
    val id: Long,
    val customerId: Long,
    val recipientEmail: String,
    val type: String,
    val channel: String,
    val status: String,
    val subject: String,
    val body: String,
    val referenceId: Long?,
    val createdAt: Instant,
    val sentAt: Instant?,
)

fun Notification.toResponse(): NotificationResponse =
    NotificationResponse(
        id = requireNotNull(id) { "Notification.id must be available before mapping to response." },
        customerId = customerId,
        recipientEmail = recipientEmail,
        type = type.name,
        channel = channel.name,
        status = status.name,
        subject = subject,
        body = body,
        referenceId = referenceId,
        createdAt = requireNotNull(createdAt) { "Notification.createdAt must be available before mapping to response." },
        sentAt = sentAt,
    )
```

Criar `src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/dto/NotificationPageResponse.kt`:

```kotlin
package com.nexus.shopping.notification.adapter.inbound.http.dto

import com.nexus.shopping.notification.domain.NotificationPage

data class NotificationPageResponse(
    val content: List<NotificationResponse>,
    val page: Int,
    val size: Int,
    val count: Int,
    val hasNext: Boolean,
)

fun NotificationPage.toResponse(): NotificationPageResponse =
    NotificationPageResponse(
        content = content.map { it.toResponse() },
        page = page,
        size = size,
        count = count,
        hasNext = hasNext,
    )
```

- [ ] **Step 4: Criar o controller**

Criar `src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/NotificationController.kt`:

```kotlin
package com.nexus.shopping.notification.adapter.inbound.http

import com.nexus.shopping.notification.adapter.inbound.http.dto.NotificationPageResponse
import com.nexus.shopping.notification.adapter.inbound.http.dto.NotificationResponse
import com.nexus.shopping.notification.adapter.inbound.http.dto.SendNotificationRequest
import com.nexus.shopping.notification.adapter.inbound.http.dto.toCommand
import com.nexus.shopping.notification.adapter.inbound.http.dto.toResponse
import com.nexus.shopping.notification.application.usecase.GetNotificationByIdUseCase
import com.nexus.shopping.notification.application.usecase.ListNotificationsByCustomerUseCase
import com.nexus.shopping.notification.application.usecase.SendNotificationUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val sendNotificationUseCase: SendNotificationUseCase,
    private val getNotificationByIdUseCase: GetNotificationByIdUseCase,
    private val listNotificationsByCustomerUseCase: ListNotificationsByCustomerUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun send(
        @RequestBody request: SendNotificationRequest,
    ): NotificationResponse = sendNotificationUseCase.send(request.toCommand()).toResponse()

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
    ): NotificationResponse = getNotificationByIdUseCase.execute(id).toResponse()

    @GetMapping
    fun listByCustomer(
        @RequestParam(required = false) customerId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): NotificationPageResponse = listNotificationsByCustomerUseCase.list(customerId, page, size).toResponse()
}
```

- [ ] **Step 5: Rodar o teste para confirmar sucesso**

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.NotificationControllerTest"`
Expected: `BUILD SUCCESSFUL`, 6 testes passando.

- [ ] **Step 6: Adicionar o teste de DTOs HTTP ao PackageStructureArchitectureTest**

Modificar `src/test/kotlin/com/nexus/shopping/PackageStructureArchitectureTest.kt`, adicionando o teste que ficou pendente da Task 2 (agora que `NotificationResponse`/`NotificationPageResponse` existem). Inserir logo apos o teste `customer http dto responses exist outside the domain package`:

```kotlin
    @Test
    fun `notification http dto responses exist outside the domain package`() {
        Class.forName("com.nexus.shopping.notification.adapter.inbound.http.dto.NotificationResponse")
        Class.forName("com.nexus.shopping.notification.adapter.inbound.http.dto.NotificationPageResponse")
    }
```

Run: `env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.PackageStructureArchitectureTest"`
Expected: `BUILD SUCCESSFUL`, todos os testes (incluindo os adicionados na Task 2) passando.

- [ ] **Step 7: Commit**

```bash
git add src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/dto/SendNotificationRequest.kt src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/dto/NotificationResponse.kt src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/dto/NotificationPageResponse.kt src/main/kotlin/com/nexus/shopping/notification/adapter/inbound/http/NotificationController.kt src/test/kotlin/com/nexus/shopping/notification/NotificationControllerTest.kt src/test/kotlin/com/nexus/shopping/PackageStructureArchitectureTest.kt
git commit -m "feat: add notification HTTP endpoints (POST, GET by id, GET by customer)"
```

---

## Task 11: Verificacao final da build completa

**Files:**
- Nenhum arquivo novo; apenas execucao e verificacao.

**Interfaces:**
- Consumes: todo o contexto `notification` criado nas Tasks 1-10.
- Produces: confirmacao de que a build completa e o `PackageStructureArchitectureTest` passam com o novo contexto.

- [ ] **Step 1: Rodar a build completa**

Run:

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Expected: `BUILD SUCCESSFUL`, sem falhas de compilacao, teste ou ktlint. Esta execucao tambem serve como confirmacao final de que a remocao do `NotificationTestConfiguration` (Task 9) nao quebrou nenhum `@SpringBootTest` pre-existente fora do contexto `notification`.

- [ ] **Step 2: Confirmar explicitamente o PackageStructureArchitectureTest**

Run:

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.PackageStructureArchitectureTest"
```

Expected: `BUILD SUCCESSFUL`, os 5 testes (incluindo os dois estendidos/adicionados na Task 2 para `notification`) passando.

- [ ] **Step 3: Confirmar todos os testes do contexto notification**

Run:

```bash
env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "com.nexus.shopping.notification.*"
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Revisar diff final**

Run:

```bash
git status
git log --oneline -12
```

Expected: working tree limpo (todos os commits das Tasks 1-10 ja aplicados), historico mostrando um commit por task.

- [ ] **Step 5: Commit (se houver qualquer ajuste residual desta verificacao)**

Se a Step 1 exigir algum ajuste de import ou formatacao (ktlint), corrigir, rodar novamente `./gradlew build` ate `BUILD SUCCESSFUL`, e commitar:

```bash
git add -A
git commit -m "chore: fix build after full verification of notification context"
```

Se nao houver nenhum ajuste, nao criar commit vazio.
