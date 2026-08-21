package com.nexus.shopping

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.springframework.stereotype.Service
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PackageStructureArchitectureTest {
    @Test
    fun `codigo de producao nao contem bounded context notification local`() {
        assertFailsWith<ClassNotFoundException> {
            Class.forName("com.nexus.shopping.notification.domain.Notification")
        }
    }

    @Test
    fun `checkout is an application process exposed through an input port`() {
        val inputPort =
            Class.forName("com.nexus.shopping.checkout.application.port.inbound.ExecuteCheckoutInputPort")
        val useCase =
            Class.forName("com.nexus.shopping.checkout.application.usecase.ExecuteCheckoutUseCase")
        val controller =
            Class.forName("com.nexus.shopping.checkout.adapter.inbound.http.CheckoutController")

        assertTrue(inputPort.isAssignableFrom(useCase))
        assertTrue(
            inputPort.isAssignableFrom(
                controller.declaredConstructors
                    .single()
                    .parameterTypes
                    .single(),
            ),
        )
        assertFalse(useCase.annotations.any { it.annotationClass.qualifiedName?.startsWith("org.springframework") == true })
    }

    @Test
    fun `Cart does not depend on Order or Checkout`() {
        assertNoDependencies(
            sourcePackage = "..cart..",
            forbiddenPackages = arrayOf("..order..", "..checkout.."),
        )
    }

    @Test
    fun `Order does not depend on Cart or Checkout`() {
        assertNoDependencies(
            sourcePackage = "..order..",
            forbiddenPackages = arrayOf("..cart..", "..checkout.."),
        )
    }

    @Test
    fun `bounded contexts do not depend on Checkout`() {
        listOf("product", "customer", "cart", "inventory", "order", "payment", "billing", "shipping").forEach { context ->
            assertNoDependencies(
                sourcePackage = "..$context..",
                forbiddenPackages = arrayOf("..checkout.."),
            )
        }
    }

    @Test
    fun `checkout application does not depend on bounded contexts, adapters, or frameworks`() {
        assertNoDependencies(
            sourcePackage = "..checkout.application..",
            forbiddenPackages =
                arrayOf(
                    "..cart..",
                    "..customer..",
                    "..inventory..",
                    "..order..",
                    "..payment..",
                    "..billing..",
                    "..shipping..",
                    "..checkout.adapter..",
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                ),
        )
    }

    @Test
    fun `Inventory does not depend on other bounded contexts or Checkout`() {
        assertNoDependencies(
            sourcePackage = "..inventory..",
            forbiddenPackages =
                arrayOf(
                    "..cart..",
                    "..customer..",
                    "..order..",
                    "..payment..",
                    "..checkout..",
                ),
        )
    }

    @Test
    fun `Payment does not depend on other bounded contexts or Checkout`() {
        assertNoDependencies(
            sourcePackage = "..payment..",
            forbiddenPackages =
                arrayOf(
                    "..cart..",
                    "..customer..",
                    "..order..",
                    "..checkout..",
                ),
        )
    }

    @Test
    fun `only bounded contexts and Checkout ACL adapters depend on context input ports`() {
        noClasses()
            .that()
            .resideOutsideOfPackages(
                "..cart..",
                "..inventory..",
                "..order..",
                "..payment..",
                "..billing..",
                "..shipping..",
                "..checkout.adapter.outbound.acl..",
            ).should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..cart.application.port.inbound..",
                "..inventory.application.port.inbound..",
                "..order.application.port.inbound..",
                "..payment.application.port.inbound..",
                "..billing.application.port.inbound..",
                "..shipping.application.port.inbound..",
            ).check(productionClasses)
    }

    @Test
    fun billingAndShippingDoNotDependOnOtherContexts() {
        val forbiddenContexts =
            arrayOf(
                "..cart..",
                "..customer..",
                "..inventory..",
                "..order..",
                "..payment..",
                "..shipping..",
                "..checkout..",
            )
        assertNoDependencies(
            sourcePackage = "..billing..",
            forbiddenPackages = forbiddenContexts,
        )
        assertNoDependencies(
            sourcePackage = "..shipping..",
            forbiddenPackages = forbiddenContexts.filterNot { it == "..shipping.." }.toTypedArray() + "..billing..",
        )
    }

    @Test
    fun `Checkout inbound adapters do not depend on outbound adapters`() {
        assertNoDependencies(
            sourcePackage = "..checkout.adapter.inbound..",
            forbiddenPackages = arrayOf("..checkout.adapter.outbound.."),
        )
    }

    @Test
    fun `top level components are free of dependency cycles`() {
        slices()
            .matching("com.nexus.shopping.(*)..")
            .should()
            .beFreeOfCycles()
            .check(productionClasses)
    }

    private fun assertNoDependencies(
        sourcePackage: String,
        forbiddenPackages: Array<String>,
    ) {
        noClasses()
            .that()
            .resideInAPackage(sourcePackage)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*forbiddenPackages)
            .check(productionClasses)
    }

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

        assertTrue(validationException.isAssignableFrom(productValidationException))
        assertTrue(notFoundException.isAssignableFrom(productNotFoundException))
        assertTrue(validationException.isAssignableFrom(customerValidationException))
        assertTrue(notFoundException.isAssignableFrom(customerNotFoundException))
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
                "com.nexus.shopping.order.application.usecase.CreateOrderUseCase",
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

    private companion object {
        val productionClasses by lazy {
            ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages("com.nexus.shopping")
        }
    }
}
