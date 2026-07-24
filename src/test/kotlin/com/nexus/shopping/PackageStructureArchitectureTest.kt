package com.nexus.shopping

import org.springframework.stereotype.Service
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
    }

    @Test
    fun `customer http dto responses exist outside the domain package`() {
        Class.forName("com.nexus.shopping.customer.adapter.inbound.http.dto.CustomerResponse")
    }

    @Test
    fun `notification http dto responses exist outside the domain package`() {
        Class.forName("com.nexus.shopping.notification.adapter.inbound.http.dto.NotificationResponse")
    }

    @Test
    fun `platform provides a generic page response reused across bounded contexts`() {
        Class.forName("com.nexus.shopping.platform.adapter.inbound.http.dto.PageResponse")
    }

    @Test
    fun `platform provides a generic page result domain type reused across bounded contexts`() {
        Class.forName("com.nexus.shopping.platform.domain.PageResult")
    }

    @Test
    fun `platform provides a shared logging context helper reused across bounded contexts`() {
        val loggerContextSource =
            java.nio.file.Path
                .of("src/main/kotlin/com/nexus/shopping/platform/application/logging/LoggerContext.kt")
                .toFile()
                .readText()

        assertTrue(loggerContextSource.contains("fun Logger.infoWithContext"))
        assertTrue(loggerContextSource.contains("fun Logger.warnWithContext"))
    }

    @Test
    fun `order use cases follow the service component pattern used by existing contexts`() {
        val orderUseCases =
            listOf(
                "com.nexus.shopping.order.application.usecase.CheckoutOrderUseCase",
                "com.nexus.shopping.order.application.usecase.GetOrderByIdUseCase",
                "com.nexus.shopping.order.application.usecase.ListOrdersByCustomerUseCase",
                "com.nexus.shopping.order.application.usecase.CancelOrderUseCase",
            )

        orderUseCases.forEach { useCase ->
            assertTrue(Class.forName(useCase).isAnnotationPresent(Service::class.java))
        }

        val orderConfiguration =
            ClassLoader.getSystemResource(
                "com/nexus/shopping/order/adapter/inbound/http/OrderApplicationConfiguration.class",
            )

        assertTrue(orderConfiguration == null)
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
