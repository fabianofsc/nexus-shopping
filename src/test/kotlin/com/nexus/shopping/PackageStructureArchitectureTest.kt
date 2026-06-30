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

        assertTrue(validationException.isAssignableFrom(productValidationException))
        assertTrue(notFoundException.isAssignableFrom(productNotFoundException))
    }

    @Test
    fun `http exception handler is platform wide and does not import product classes`() {
        val handler = Class.forName("com.nexus.shopping.platform.adapter.inbound.http.ApiExceptionHandler")
        assertTrue(handler.simpleName == "ApiExceptionHandler")

        val handlerSource = java.nio.file.Path.of(
            "src/main/kotlin/com/nexus/shopping/platform/adapter/inbound/http/ApiExceptionHandler.kt",
        ).toFile().readText()

        assertFalse(handlerSource.contains("com.nexus.shopping.product"))
    }

    @Test
    fun `product http dto responses exist outside the domain package`() {
        Class.forName("com.nexus.shopping.product.adapter.inbound.http.dto.ProductResponse")
        Class.forName("com.nexus.shopping.product.adapter.inbound.http.dto.ProductPageResponse")
    }

    @Test
    fun `codebase does not use a shared package for cross cutting structure`() {
        val sourceRoot = java.nio.file.Path.of("src/main/kotlin/com/nexus/shopping")
        val sharedPackageExists = java.nio.file.Files.walk(sourceRoot).use { paths ->
            paths
                .filter { path -> java.nio.file.Files.isRegularFile(path) && path.toString().endsWith(".kt") }
                .anyMatch { path ->
                    path.toString().contains("/shared/") || path.toFile().readText().contains("com.nexus.shopping.shared")
                }
        }

        assertFalse(sharedPackageExists)
    }
}
