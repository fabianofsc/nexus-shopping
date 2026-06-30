package com.nexus.shopping.shared.observability

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CorrelationIdProviderTest {

    private val provider = CorrelationIdProvider()

    @Test
    fun `header ausente gera UUID`() {
        val result = provider.resolveCorrelationId(null)
        assertTrue(result.isNotEmpty())
        assertTrue(result.length == 36) // Formato UUID v4
    }

    @Test
    fun `header em branco gera UUID`() {
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        val result1 = provider.resolveCorrelationId("")
        val result2 = provider.resolveCorrelationId("   ")
        assertTrue(result1.matches(uuidRegex))
        assertTrue(result2.matches(uuidRegex))
    }

    @Test
    fun `header válido é preservado`() {
        val valid = "trace-001-xyz"
        val result = provider.resolveCorrelationId(valid)
        assertEquals(valid, result)
    }

    @Test
    fun `caracteres inválidos geram UUID`() {
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        val result = provider.resolveCorrelationId("trace\ninjection")
        assertTrue(result.matches(uuidRegex))
    }

    @Test
    fun `header excedendo 128 caracteres gera UUID`() {
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        val result = provider.resolveCorrelationId("x".repeat(129))
        assertTrue(result.matches(uuidRegex))
    }

    @Test
    fun `header com caracteres especiais permitidos é preservado`() {
        val valid = "service.name-123_prod:v1"
        val result = provider.resolveCorrelationId(valid)
        assertEquals(valid, result)
    }
}
