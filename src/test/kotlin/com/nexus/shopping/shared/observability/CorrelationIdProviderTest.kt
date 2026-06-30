package com.nexus.shopping.shared.observability

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
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
        val result1 = provider.resolveCorrelationId("")
        val result2 = provider.resolveCorrelationId("   ")
        assertTrue(result1.isNotEmpty())
        assertTrue(result2.isNotEmpty())
        assertNotEquals(result1, result2) // Cada um gera um UUID novo
    }

    @Test
    fun `header válido é preservado`() {
        val valid = "trace-001-xyz"
        val result = provider.resolveCorrelationId(valid)
        assertEquals(valid, result)
    }

    @Test
    fun `caracteres inválidos geram UUID`() {
        val invalid = "trace\ninjection"
        val result = provider.resolveCorrelationId(invalid)
        assertNotEquals(invalid, result)
        assertTrue(result.length == 36)
    }

    @Test
    fun `header excedendo 128 caracteres gera UUID`() {
        val oversized = "x".repeat(129)
        val result = provider.resolveCorrelationId(oversized)
        assertNotEquals(oversized, result)
        assertTrue(result.length == 36)
    }

    @Test
    fun `header com caracteres especiais permitidos é preservado`() {
        val valid = "service.name-123_prod:v1"
        val result = provider.resolveCorrelationId(valid)
        assertEquals(valid, result)
    }
}
