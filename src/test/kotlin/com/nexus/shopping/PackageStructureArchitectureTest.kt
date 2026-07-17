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
    fun `notification http dto responses exist outside the domain package`() {
        Class.forName("com.nexus.shopping.notification.adapter.inbound.http.dto.NotificationResponse")
        Class.forName("com.nexus.shopping.notification.adapter.inbound.http.dto.NotificationPageResponse")
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
