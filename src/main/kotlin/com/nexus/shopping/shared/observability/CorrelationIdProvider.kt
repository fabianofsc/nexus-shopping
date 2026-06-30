package com.nexus.shopping.shared.observability

import java.util.UUID

class CorrelationIdProvider {

    companion object {
        private const val MAX_LENGTH = 128
        private val ALLOWED_PATTERN = Regex("^[a-zA-Z0-9._:\\-]*$")
    }

    fun resolveCorrelationId(headerValue: String?): String {
        if (headerValue == null || headerValue.isBlank()) {
            return generateUUID()
        }

        val trimmed = headerValue.trim()

        // Validar tamanho
        if (trimmed.length > MAX_LENGTH) {
            return generateUUID()
        }

        // Validar caracteres (apenas letras, dígitos, . _ - :)
        if (!trimmed.matches(ALLOWED_PATTERN)) {
            return generateUUID()
        }

        return trimmed
    }

    private fun generateUUID(): String = UUID.randomUUID().toString()
}
